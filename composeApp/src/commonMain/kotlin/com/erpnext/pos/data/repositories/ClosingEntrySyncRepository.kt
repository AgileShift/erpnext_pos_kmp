package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.data.mappers.buildClosingEntryDto
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis

class ClosingEntrySyncRepository(
    private val api: APIService,
    private val cashboxDao: CashboxDao,
    private val openingEntryLinkDao: POSOpeningEntryLinkDao,
    private val openingEntryDao: POSOpeningEntryDao,
    private val closingDao: POSClosingEntryDao,
    private val salesInvoiceDao: SalesInvoiceDao
) {
    suspend fun pushPending(): Boolean {
        val pending = cashboxDao.getClosedPendingSync()
        if (pending.isEmpty()) return false
        var hasChanges = false

        pending.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            val localClosingName = cashbox.closingEntryId
            val remoteOpeningName = openingEntryLinkDao.getRemoteOpeningEntryName(cashbox.localId)
                ?: cashbox.openingEntryId?.takeIf {
                    it.isNotBlank() && !it.startsWith("LOCAL-", ignoreCase = true)
                }
            if (remoteOpeningName.isNullOrBlank()) {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: missing remote opening for cashbox ${cashbox.localId}"
                )
                return@forEach
            }
            val periodEnd = cashbox.periodEndDate
            if (periodEnd.isNullOrBlank()) return@forEach
            val startMillis = parseErpDateTimeToEpochMillis(cashbox.periodStartDate) ?: return@forEach
            val endMillis = parseErpDateTimeToEpochMillis(periodEnd) ?: return@forEach
            val shiftInvoices = cashbox.openingEntryId?.takeIf { it.isNotBlank() }?.let {
                salesInvoiceDao.getInvoicesForOpeningEntry(it)
            } ?: salesInvoiceDao.getInvoicesForShift(
                profileId = cashbox.posProfile,
                startMillis = startMillis,
                endMillis = endMillis
            )
            ensureRemoteOpeningEntry(remoteOpeningName, cashbox.localId)
            val dto = buildClosingEntryDto(
                cashbox = cashbox,
                openingEntryId = remoteOpeningName,
                postingDate = periodEnd,
                periodEndDate = periodEnd,
                balanceDetails = wrapper.details,
                invoices = shiftInvoices
            )

            val existingRemote = localClosingName
                ?.takeIf { it.isNotBlank() && !it.startsWith("LOCAL-", ignoreCase = true) }
            val response = if (existingRemote == null) {
                runCatching { api.closeCashbox(dto) }
                    .onFailure { AppLogger.warn("ClosingEntrySyncRepository create failed", it) }
                    .getOrNull()
            } else {
                com.erpnext.pos.remoteSource.dto.POSClosingEntryResponse(existingRemote)
            }
            if (response == null) return@forEach
            val submitOk = runCatching { api.submitPOSClosingEntry(response.name) }
                .onFailure { AppLogger.warn("ClosingEntrySyncRepository submit failed", it) }
                .getOrNull()
            val pendingSync = submitOk == null
            cashboxDao.updateStatus(
                cashbox.localId,
                status = false,
                pceId = response.name,
                endDate = periodEnd,
                pendingSync = pendingSync
            )
            closingDao.insert(dto.toEntity(response.name, pendingSync = pendingSync))
            if (!pendingSync && localClosingName?.startsWith("LOCAL-", ignoreCase = true) == true) {
                closingDao.delete(localClosingName)
            }
            hasChanges = true
        }

        return hasChanges
    }

    private suspend fun ensureRemoteOpeningEntry(remoteName: String, cashboxId: Long) {
        val existing = openingEntryDao.getByName(remoteName)
        if (existing != null) return
        val link = openingEntryLinkDao.getByCashboxId(cashboxId) ?: return
        val local = openingEntryDao.getByName(link.localOpeningEntryName) ?: return
        openingEntryDao.insert(local.copy(name = remoteName, pendingSync = false))
    }
}
