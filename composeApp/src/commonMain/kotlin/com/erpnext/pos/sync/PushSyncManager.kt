package com.erpnext.pos.sync

import com.erpnext.pos.data.repositories.v2.DeliveryNoteRepository
import com.erpnext.pos.data.repositories.v2.PaymentEntryRepository
import com.erpnext.pos.data.repositories.v2.QuotationRepository
import com.erpnext.pos.data.repositories.v2.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.v2.SalesOrderRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.sync.SyncDocType
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationCreateDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderCreateDto
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private typealias PushHandler = suspend (SyncContext) -> Boolean

@OptIn(ExperimentalTime::class)
class PushSyncManager(
    private val syncRepository: SyncRepository,
    private val quotationRepository: QuotationRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val deliveryNoteRepository: DeliveryNoteRepository,
    private val paymentEntryRepository: PaymentEntryRepository,
    private val salesInvoiceRepository: SalesInvoiceRepository,
    private val api: APIServiceV2
) {
    private data class PushSyncTask(
        val docType: SyncDocType,
        val displayName: String,
        val handler: PushHandler
    )

    private val tasks = listOf(
        PushSyncTask(
            SyncDocType.QUOTATION,
            "Cotizaciones",
            this::pushQuotations
        ),
        PushSyncTask(
            SyncDocType.SALES_ORDER,
            "Ordenes de venta",
            this::pushSalesOrders
        ),
        PushSyncTask(
            SyncDocType.DELIVERY_NOTE,
            "Notas de entrega",
            this::pushDeliveryNotes
        ),
        PushSyncTask(
            SyncDocType.SALES_INVOICE,
            "Facturas locales",
            this::pushInvoices
        ),
        PushSyncTask(
            SyncDocType.PAYMENT_ENTRY,
            "Registros de pago",
            this::pushPaymentEntries
        )
    )

    suspend fun runPushQueue(ctx: SyncContext, onDocType: (String) -> Unit): Boolean {
        AppSentry.breadcrumb("PushSyncManager.runPushQueue start")
        AppLogger.info("PushSyncManager: starting push queue for ${ctx.companyId}")
        var hasChanges = false
        tasks.forEach { task ->
            onDocType(task.displayName)
            val changed = executeTask(ctx, task)
            if (changed) hasChanges = true
        }
        AppSentry.breadcrumb("PushSyncManager.runPushQueue end")
        AppLogger.info("PushSyncManager: completed push queue (changes=$hasChanges)")
        return hasChanges
    }

    private suspend fun executeTask(ctx: SyncContext, task: PushSyncTask): Boolean {
        syncRepository.setInProgress(ctx.instanceId, ctx.companyId, task.docType.value, true)
        return try {
            val changed = task.handler(ctx)
            syncRepository.markPushSuccess(ctx.instanceId, ctx.companyId, task.docType.value)
            syncRepository.refreshCounters(
                ctx.instanceId,
                ctx.companyId,
                task.docType.value,
                Clock.System.now().epochSeconds
            )
            changed
        } catch (e: Throwable) {
            syncRepository.markFailure(ctx.instanceId, ctx.companyId, task.docType.value, e)
            throw e
        } finally {
            syncRepository.setInProgress(ctx.instanceId, ctx.companyId, task.docType.value, false)
        }
    }

    private suspend fun pushQuotations(ctx: SyncContext): Boolean {
        val pending = quotationRepository.pushPending(ctx)
        if (pending.isEmpty()) return false
        val failedIds = mutableListOf<String>()
        pending.forEach { item ->
            try {
                val response = api.createDoc<QuotationCreateDto>(ERPDocType.Quotation, item.payload)
                quotationRepository.markSynced(
                    ctx.instanceId,
                    ctx.companyId,
                    item.localId,
                    response.name,
                    response.modified
                )
            } catch (e: Throwable) {
                AppSentry.capture(e, "PushSyncManager: quotation ${item.localId} failed")
                AppLogger.warn("PushSyncManager: quotation ${item.localId} failed", e)
                quotationRepository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                failedIds += item.localId
            }
        }
        if (failedIds.isNotEmpty()) {
            throw IllegalStateException("Cotizaciones push failed for ${failedIds.joinToString()}")
        }
        return true
    }

    private suspend fun pushSalesOrders(ctx: SyncContext): Boolean {
        val pending = salesOrderRepository.pushPending(ctx)
        if (pending.isEmpty()) return false
        val failedIds = mutableListOf<String>()
        pending.forEach { item ->
            try {
                val response = api.createDoc<SalesOrderCreateDto>(ERPDocType.SalesOrder, item.payload)
                salesOrderRepository.markSynced(
                    ctx.instanceId,
                    ctx.companyId,
                    item.localId,
                    response.name,
                    response.modified
                )
            } catch (e: Throwable) {
                AppSentry.capture(e, "PushSyncManager: sales order ${item.localId} failed")
                AppLogger.warn("PushSyncManager: sales order ${item.localId} failed", e)
                salesOrderRepository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                failedIds += item.localId
            }
        }
        if (failedIds.isNotEmpty()) {
            throw IllegalStateException("Ordenes de venta push failed for ${failedIds.joinToString()}")
        }
        return true
    }

    private suspend fun pushDeliveryNotes(ctx: SyncContext): Boolean {
        val pending = deliveryNoteRepository.pushPending(ctx)
        if (pending.isEmpty()) return false
        val failedIds = mutableListOf<String>()
        pending.forEach { item ->
            try {
                val response = api.createDoc<DeliveryNoteCreateDto>(ERPDocType.DeliveryNote, item.payload)
                deliveryNoteRepository.markSynced(
                    ctx.instanceId,
                    ctx.companyId,
                    item.localId,
                    response.name,
                    response.modified
                )
            } catch (e: Throwable) {
                AppSentry.capture(e, "PushSyncManager: delivery note ${item.localId} failed")
                AppLogger.warn("PushSyncManager: delivery note ${item.localId} failed", e)
                deliveryNoteRepository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                failedIds += item.localId
            }
        }
        if (failedIds.isNotEmpty()) {
            throw IllegalStateException("Delivery notes push failed for ${failedIds.joinToString()}")
        }
        return true
    }

    private suspend fun pushPaymentEntries(ctx: SyncContext): Boolean {
        val pending = paymentEntryRepository.pushPending(ctx)
        if (pending.isEmpty()) return false
        val failedIds = mutableListOf<String>()
        pending.forEach { item ->
            try {
                val response = api.createDoc<PaymentEntryCreateDto>(ERPDocType.PaymentEntry, item.payload)
                paymentEntryRepository.markSynced(
                    ctx.instanceId,
                    ctx.companyId,
                    item.localId,
                    response.name,
                    response.modified
                )
            } catch (e: Throwable) {
                AppSentry.capture(e, "PushSyncManager: payment entry ${item.localId} failed")
                AppLogger.warn("PushSyncManager: payment entry ${item.localId} failed", e)
                paymentEntryRepository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                failedIds += item.localId
            }
        }
        if (failedIds.isNotEmpty()) {
            throw IllegalStateException("Payment entries push failed for ${failedIds.joinToString()}")
        }
        return true
    }

    private suspend fun pushInvoices(ctx: SyncContext): Boolean {
        return salesInvoiceRepository.syncOutbox(ctx)
    }
}
