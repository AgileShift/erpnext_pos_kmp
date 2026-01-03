package com.erpnext.pos.data.repositories

import com.erpnext.pos.base.Resource
import com.erpnext.pos.base.networkBoundResource
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.repositories.ICustomerRepository
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.remoteSource.datasources.CustomerRemoteSource
import com.erpnext.pos.remoteSource.dto.CustomerCreditLimitDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.mapper.toBO
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.sync.SyncTTL
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.collections.map

class CustomerRepository(
    private val remoteSource: CustomerRemoteSource,
    private val localSource: CustomerLocalSource,
    private val context: CashBoxManager
) : ICustomerRepository {

    override suspend fun getCustomers(
        search: String?, state: String?
    ): Flow<List<CustomerBO>> {
        val territory: String? = context.requireContext().route
        return networkBoundResource(query = {
            // 🔹 Consultamos localmente según filtros dinámicos
            when {
                state.isNullOrEmpty() && search.isNullOrEmpty() -> localSource.getAll()

                state.isNullOrEmpty() -> localSource.getAllFiltered(search!!)

                search.isNullOrEmpty() -> {
                    if (state == "Todos") localSource.getAll()
                    else localSource.getByCustomerState(state)
                }

                else -> localSource.getAll()
            }.map { list -> list.map { it.toBO() } }
        }, fetch = {
            // 🔹 Obtenemos datos remotos
            remoteSource.fetchCustomers(territory)
        }, saveFetchResult = { remoteData ->
            // 🔹 Guardamos resultados en DB local
            val invoices =
                remoteSource.fetchInvoices(context.requireContext().profileName).toEntities()
            localSource.saveInvoices(invoices)

            // Fetch all outstanding invoices once
            val allOutstanding = remoteSource.fetchAllOutstandingInvoices()
            val outstandingByCustomer = allOutstanding.groupBy { it.customer }

            coroutineScope {
                val contextCompany = context.requireContext().company
                val entities = remoteData.map { dto ->
                    async {
                        val customerInvoices = outstandingByCustomer[dto.name] ?: emptyList()
                        val totalOutstanding =
                            customerInvoices.sumOf { it.grandTotal - it.paidAmount }
                        val resolvedLimit = dto.creditLimitForCompany(contextCompany)
                        val creditLimit = resolvedLimit?.creditLimit //?: dto.creditLimit
                        val availableCredit = creditLimit?.let { it - totalOutstanding }
                        //val address = remoteSource.getCustomerAddress(dto.name)
                        //val contact = remoteSource.getCustomerContact(dto.name)

                        dto.toEntity(
                            creditLimit = creditLimit,
                            availableCredit = availableCredit,
                            pendingInvoicesCount = customerInvoices.size,
                            totalPendingAmount = totalOutstanding,
                            state = if (totalOutstanding > 0) "Pendientes" else "Sin Pendientes",
                        )
                    }
                }.awaitAll()
                localSource.insertAll(entities)
            }
        }, shouldFetch = { localData ->
            localData.isEmpty() || SyncTTL.isExpired(localData.maxOf {
                it.lastSyncedAt?.toDouble() ?: 0.0
            }.toLong())
        }, onFetchFailed = { e ->
            println("⚠️ Error al sincronizar clientes: ${e.message}")
        }).map { resource -> resource.data ?: emptyList() }
    }

    override suspend fun getCustomerByName(name: String): CustomerBO? {
        return localSource.getByName(name)?.toBO()
    }

    override suspend fun sync(): Flow<Resource<List<CustomerBO>>> {
        val territory: String? = context.requireContext().route
        return networkBoundResource(query = { flowOf(emptyList<CustomerDto>().toBO()) }, fetch = {
            remoteSource.fetchCustomers(territory)
        }, shouldFetch = { localData ->
            true
            /*localData.isEmpty() ||
                        SyncTTL.isExpired(localData.maxOf { it.lastSyncedAt?.toDouble() ?: 0.0 }
                            .toLong())*/
        }, saveFetchResult = { remoteData ->
            // 🔹 Guardamos resultados en DB local
            val invoices =
                remoteSource.fetchInvoices(context.requireContext().profileName).toEntities()
            localSource.saveInvoices(invoices)

            // Fetch all outstanding invoices once
            val allOutstanding = remoteSource.fetchAllOutstandingInvoices()
            val outstandingByCustomer = allOutstanding.groupBy { it.customer }

            coroutineScope {
                val entities = remoteData.map { dto ->
                    async {
                        val customerInvoices = outstandingByCustomer[dto.name] ?: emptyList()
                        val totalOutstanding =
                            customerInvoices.sumOf { it.grandTotal - it.paidAmount }
                        val creditLimit = dto.creditLimits
                        val available = if (creditLimit.isNotEmpty()) (creditLimit[0].creditLimit
                            ?: 0.0) - totalOutstanding else 0.0
                        //val address = remoteSource.getCustomerAddress(dto.name)
                        //val contact = remoteSource.getCustomerContact(dto.name)

                        dto.toEntity(
                            creditLimit = if (creditLimit.isNotEmpty()) creditLimit[0].creditLimit else 0.0,
                            availableCredit = available, //availableCredit,
                            pendingInvoicesCount = customerInvoices.size,
                            totalPendingAmount = totalOutstanding,
                            state = if (totalOutstanding > 0) "Pendientes" else "Sin Pendientes",
                            //address = null, //address ?: "",
                            //contact = null
                        )
                    }
                }.awaitAll()
                localSource.insertAll(entities)
            }
        }, onFetchFailed = { it.printStackTrace() })
    }
}
