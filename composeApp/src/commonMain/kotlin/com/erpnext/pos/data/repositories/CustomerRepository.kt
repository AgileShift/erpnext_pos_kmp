package com.erpnext.pos.data.repositories

import com.erpnext.pos.base.Resource
import com.erpnext.pos.base.networkBoundResource
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.repositories.ICustomerRepository
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.datasources.CustomerRemoteSource
import com.erpnext.pos.remoteSource.dto.CustomerCreditLimitDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.mapper.toBO
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.sync.SyncTTL
import com.erpnext.pos.utils.RepoTrace
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.collections.map

class CustomerRepository(
    private val remoteSource: CustomerRemoteSource,
    private val localSource: CustomerLocalSource,
    private val outboxLocalSource: com.erpnext.pos.localSource.datasources.CustomerOutboxLocalSource,
    private val context: CashBoxManager
) : ICustomerRepository {
    private companion object {
        const val ONLINE_REFRESH_TTL_MINUTES = 5L
    }

    suspend fun rebuildAllCustomerSummaries() {
        val customers = localSource.getAll().first()
        customers.forEach { customer ->
            refreshCustomerSummaryWithRates(customer.name)
        }
    }

    override suspend fun getCustomers(
        search: String?, state: String?
    ): Flow<List<CustomerBO>> {
        RepoTrace.breadcrumb("CustomerRepository", "getCustomers", "search=$search state=$state")
        return when {
            state.isNullOrEmpty() && search.isNullOrEmpty() -> localSource.getAll()
            state.isNullOrEmpty() -> localSource.getAllFiltered(search ?: "")
            search.isNullOrEmpty() -> if (state == "Todos") localSource.getAll()
            else localSource.getByCustomerState(state)
            else -> localSource.getAll()
        }.map { list -> list.map { it.toBO() } }
    }

    override suspend fun getCustomerByName(name: String): CustomerBO? {
        return localSource.getByName(name)?.toBO()
    }

    override suspend fun sync(): Flow<Resource<List<CustomerBO>>> {
        val territory: String? = context.requireContext().route
        RepoTrace.breadcrumb("CustomerRepository", "sync")
        return networkBoundResource(query = { flowOf(emptyList<CustomerDto>().toBO()) }, fetch = {
            val profileId = context.requireContext().profileName
            val recentPaidOnly = localSource.countInvoices() > 0
            remoteSource.fetchCustomersBootstrapSnapshot(
                profileName = profileId,
                territory = territory,
                recentPaidOnly = recentPaidOnly
            )
        }, shouldFetch = { localData ->
            true
            /*localData.isEmpty() ||
                        SyncTTL.isExpired(localData.maxOf { it.lastSyncedAt?.toDouble() ?: 0.0 }
                            .toLong())*/
        }, saveFetchResult = { remoteData ->
            val profileId = context.requireContext().profileName
            val invoices = remoteData.invoices
            val entities = invoices.toEntities()
            localSource.saveInvoices(entities.map { normalizeBaseOutstanding(mergeLocalInvoiceFields(it)) })
            backfillMissingInvoiceItems(profileId)

            // Fetch all outstanding invoices once
            val allOutstanding = remoteData.invoices.filter { invoice ->
                val outstanding = invoice.outstandingAmount ?: (invoice.grandTotal - (invoice.paidAmount ?: 0.0))
                outstanding > 0.0
            }
            val remoteOutstandingNames = allOutstanding.mapNotNull { it.name }.toSet()
            val localOutstanding = localSource.getOutstandingInvoiceNames()
            val missingOutstanding = localOutstanding.filterNot { remoteOutstandingNames.contains(it) }
            missingOutstanding.forEach { invoiceName ->
                val local = localSource.getInvoiceByName(invoiceName)
                val customerId = local?.invoice?.customer
                val remote = remoteSource.fetchInvoiceByNameSmart(invoiceName)
                if (remote != null) {
                    localSource.saveInvoices(listOf(remote.toEntity()))
                } else {
                    localSource.deleteInvoiceById(invoiceName)
                }
                customerId?.let { refreshCustomerSummaryWithRates(it) }
            }

            coroutineScope {
                val entities = remoteData.customers.map { dto ->
                    async {
                        val creditLimit = dto.creditLimits
                        val available =
                            if (creditLimit.isNotEmpty()) (creditLimit.firstOrNull()?.creditLimit
                                ?: 0.0) else 0.0
                        //val address = remoteSource.getCustomerAddress(dto.name)
                        //val contact = remoteSource.getCustomerContact(dto.name)

                        dto.toEntity(
                            creditLimit = if (creditLimit.isNotEmpty()) creditLimit[0].creditLimit else 0.0,
                            availableCredit = available, //availableCredit,
                            pendingInvoicesCount = 0,
                            totalPendingAmount = 0.0,
                            state = "Sin Pendientes",
                            //address = null, //address ?: "",
                            //contact = null
                        )
                    }
                }.awaitAll()
                localSource.insertAll(entities)
                val ids = entities.map { it.name }
                localSource.deleteMissing(ids)
                outboxLocalSource.deleteMissingCustomerIds(ids)
                entities.map { it.name }.distinct().forEach { customerId ->
                    refreshCustomerSummaryWithRates(customerId)
                }
            }
        }, onFetchFailed = {
            RepoTrace.capture("CustomerRepository", "sync", it)
            it.printStackTrace()
        })
    }

    private suspend fun refreshCustomerSummaryWithRates(customerId: String) {
        val invoices = localSource.getOutstandingInvoicesForCustomer(customerId)
        if (invoices.isEmpty()) {
            localSource.updateSummary(
                customerId = customerId,
                totalPendingAmount = 0.0,
                pendingInvoicesCount = 0,
                currentBalance = 0.0,
                availableCredit = null,
                state = "Sin Pendientes"
            )
            return
        }
        val ctx = context.getContext()
        val baseCurrency = ctx?.companyCurrency ?: ctx?.currency ?: "NIO"
        var totalPending = 0.0
        invoices.forEach { wrapper ->
            val invoice = wrapper.invoice
            val receivableCurrency = invoice.partyAccountCurrency ?: invoice.currency
            val outstanding =
                (invoice.outstandingAmount ?: invoice.baseOutstandingAmount)
                    ?.coerceAtLeast(0.0) ?: 0.0
            val rate = when {
                receivableCurrency.equals(baseCurrency, ignoreCase = true) -> 1.0
                else -> context.resolveExchangeRateBetween(
                    receivableCurrency,
                    baseCurrency,
                    allowNetwork = false
                )
                    ?: com.erpnext.pos.utils.CurrencyService.resolveReceivableToInvoiceRateUnified(
                        invoiceCurrency = invoice.currency,
                        receivableCurrency = receivableCurrency,
                        conversionRate = invoice.conversionRate,
                        customExchangeRate = invoice.customExchangeRate,
                        posCurrency = ctx?.currency,
                        posExchangeRate = ctx?.exchangeRate,
                        rateResolver = { from, to ->
                            context.resolveExchangeRateBetween(from, to, allowNetwork = false)
                        }
                    )
                    ?: 1.0
            }
            totalPending += (outstanding * rate)
        }
        val pendingCount = invoices.count { it.invoice.outstandingAmount > 0.0 }
        val customer = localSource.getByName(customerId)
        val creditLimit = customer?.creditLimit
        val availableCredit = creditLimit?.let { it - totalPending }
        val state = if (totalPending > 0.0) "Pendientes" else "Sin Pendientes"
        localSource.updateSummary(
            customerId = customerId,
            totalPendingAmount = roundToCurrency(totalPending),
            pendingInvoicesCount = pendingCount,
            currentBalance = roundToCurrency(totalPending),
            availableCredit = availableCredit?.let { roundToCurrency(it) },
            state = state
        )
    }

    private suspend fun mergeLocalInvoiceFields(
        payload: SalesInvoiceWithItemsAndPayments
    ): SalesInvoiceWithItemsAndPayments {
        val invoiceName = payload.invoice.invoiceName?.trim().orEmpty()
        if (invoiceName.isBlank()) return payload
        val local = localSource.getInvoiceByName(invoiceName)?.invoice ?: return payload
        val mergedInvoice = payload.invoice.copy(
            profileId = payload.invoice.profileId?.takeIf { it.isNotBlank() } ?: local.profileId,
            posOpeningEntry = payload.invoice.posOpeningEntry?.takeIf { it.isNotBlank() }
                ?: local.posOpeningEntry,
            warehouse = payload.invoice.warehouse?.takeIf { it.isNotBlank() } ?: local.warehouse,
            partyAccountCurrency = payload.invoice.partyAccountCurrency?.takeIf { it.isNotBlank() }
                ?: local.partyAccountCurrency
        )
        return payload.copy(invoice = mergedInvoice)
    }

    private suspend fun backfillMissingInvoiceItems(profileId: String, limit: Int = 50) {
        val missing = localSource.getInvoiceNamesMissingItems(profileId, limit)
        if (missing.isEmpty()) return
        missing.forEach { invoiceName ->
            val remote = remoteSource.fetchInvoiceByNameSmart(invoiceName) ?: return@forEach
            val merged = normalizeBaseOutstanding(mergeLocalInvoiceFields(remote.toEntity()))
            localSource.saveInvoices(listOf(merged))
        }
    }

    private fun normalizeBaseOutstanding(
        payload: SalesInvoiceWithItemsAndPayments
    ): SalesInvoiceWithItemsAndPayments {
        val companyCurrency = context.getContext()?.companyCurrency ?: return payload
        val partyCurrency = payload.invoice.partyAccountCurrency ?: return payload
        if (!partyCurrency.equals(companyCurrency, ignoreCase = true)) return payload
        val invoice = payload.invoice.copy(
            baseOutstandingAmount = payload.invoice.outstandingAmount,
            basePaidAmount = payload.invoice.paidAmount
        )
        return payload.copy(invoice = invoice)
    }

    suspend fun fetchInvoicesForCustomerPeriod(
        customerId: String,
        startDate: String,
        endDate: String
    ): List<SalesInvoiceBO> {
        val profileId = context.requireContext().profileName
        val invoices = remoteSource.fetchInvoicesForCustomerPeriod(
            customerId,
            startDate,
            endDate,
            profileId
        )
        if (invoices.isEmpty()) return emptyList()
        return invoices.mapNotNull { dto ->
            runCatching {
                val entity = dto.toEntity()
                entity.toBO()
            }.getOrNull()
        }
    }

    suspend fun fetchLocalInvoicesForCustomerPeriod(
        customerId: String,
        startDate: String,
        endDate: String
    ): List<SalesInvoiceBO> {
        val invoices = localSource.getInvoicesForCustomerInRange(
            customerName = customerId,
            startDate = startDate,
            endDate = endDate
        )
        val invoicesBo = invoices.toBO()
        return invoicesBo
    }
}

//TODO: Las facturas se estan guardando con currency USD, verificar
