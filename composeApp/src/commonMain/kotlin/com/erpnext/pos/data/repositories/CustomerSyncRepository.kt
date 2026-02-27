package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.CustomerCreatePayload
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.datasources.CustomerOutboxLocalSource
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.CustomerOutboxEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.CustomerCreateDto
import com.erpnext.pos.remoteSource.dto.CustomerCreditLimitCreateDto
import com.erpnext.pos.remoteSource.sdk.json
import com.erpnext.pos.sync.PushQueueReport
import com.erpnext.pos.sync.PushSyncConflict
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.views.CashBoxManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CustomerSyncRepository(
    private val api: APIService,
    private val outboxLocalSource: CustomerOutboxLocalSource,
    private val customerLocalSource: CustomerLocalSource,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val cashBoxManager: CashBoxManager
) {
    suspend fun enqueueCustomer(entity: CustomerEntity, payload: CustomerCreatePayload) {
        customerLocalSource.insert(entity)
        val outbox = CustomerOutboxEntity(
            localId = entity.name,
            customerLocalId = entity.name,
            payloadJson = json.encodeToString(payload),
            status = "Pending",
            attempts = 0,
            lastError = null,
            remoteId = null,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            lastAttemptAt = null
        )
        outboxLocalSource.insert(outbox)
    }

    suspend fun pushPending(): Boolean = pushPendingWithReport().hasChanges

    suspend fun pushPendingWithReport(): PushQueueReport {
        val pending = outboxLocalSource.getPending()
        if (pending.isEmpty()) return PushQueueReport.EMPTY
        var hasChanges = false
        val conflicts = mutableListOf<PushSyncConflict>()
        val attemptAt = Clock.System.now().toEpochMilliseconds()
        pending.forEach { item ->
            try {
                val payload = json.decodeFromString<CustomerCreatePayload>(item.payloadJson)
                item.remoteId ?: run {
                    val creditLimits = payload.creditLimit?.let {
                        listOf(
                            CustomerCreditLimitCreateDto(
                                company = payload.representsCompany
                                    ?: cashBoxManager.requireContext().company,
                                creditLimit = it
                            )
                        )
                    }
                    val response = api.createCustomer(
                        CustomerCreateDto(
                            customerName = payload.customerName,
                            customerType = payload.customerType,
                            customerGroup = payload.customerGroup,
                            territory = payload.territory,
                            defaultCurrency = payload.defaultCurrency,
                            defaultPriceList = payload.defaultPriceList,
                            mobileNo = payload.mobileNo ?: payload.phone,
                            emailId = payload.email,
                            taxId = payload.taxId,
                            taxCategory = payload.taxCategory,
                            isInternalCustomer = payload.isInternalCustomer,
                            representsCompany = payload.representsCompany,
                            creditLimits = creditLimits,
                            paymentTerms = payload.paymentTerms,
                            customerDetails = payload.notes
                        )
                    )
                    outboxLocalSource.updateRemoteId(item.localId, response.name)
                    customerLocalSource.updateCustomerId(
                        oldId = item.customerLocalId,
                        newId = response.name,
                        customerName = payload.customerName
                    )
                    salesInvoiceDao.updateCustomerId(
                        oldId = item.customerLocalId,
                        newId = response.name,
                        customerName = payload.customerName
                    )
                    response.name
                }

                outboxLocalSource.updateStatus(
                    localId = item.localId,
                    status = "Synced",
                    error = null,
                    attemptIncrement = 1,
                    attemptAt = attemptAt
                )
                hasChanges = true
            } catch (e: Exception) {
                AppSentry.capture(e, "CustomerSyncRepository.pushPending failed")
                AppLogger.warn("CustomerSyncRepository.pushPending failed", e)
                if (isDuplicateCustomerError(e)) {
                    conflicts += PushSyncConflict(
                        docType = "Customer",
                        localId = item.customerLocalId,
                        remoteId = item.remoteId,
                        reason = "Cliente local pendiente podría existir en remoto (error de duplicado)."
                    )
                }
                outboxLocalSource.updateStatus(
                    localId = item.localId,
                    status = "Failed",
                    error = e.message,
                    attemptIncrement = 1,
                    attemptAt = attemptAt
                )
            }
        }
        return PushQueueReport(
            hasChanges = hasChanges,
            conflicts = conflicts
        )
    }

    private fun isDuplicateCustomerError(error: Throwable): Boolean {
        val msg = buildList {
            error.message?.let { add(it) }
            (error as? com.erpnext.pos.remoteSource.sdk.FrappeException)
                ?.errorResponse
                ?._server_messages
                ?.let { add(it) }
        }
            .joinToString(" | ")
            .lowercase()
        return msg.contains("duplicate") ||
            msg.contains("already exists") ||
            msg.contains("ya existe") ||
            msg.contains("must be unique")
    }
}
