package com.erpnext.pos.sync

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry

class LegacyPushSyncManager(
    private val invoiceRepository: SalesInvoiceRepository
) : PushSyncRunner {

    override suspend fun runPushQueue(ctx: SyncContext, onDocType: (String) -> Unit): Boolean {
        onDocType("Facturas locales")
        return try {
            val pending = invoiceRepository.getPendingSyncInvoices()
            if (pending.isEmpty()) return false
            invoiceRepository.syncPendingInvoices()
            true
        } catch (e: Throwable) {
            AppSentry.capture(e, "LegacyPushSyncManager: invoices push failed")
            AppLogger.warn("LegacyPushSyncManager: invoices push failed", e)
            throw e
        }
    }
}
