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
    suspend fun reconcileRemoteClosingsForActiveCashboxes(): Boolean {
        val active = cashboxDao.getActiveCashboxes()
        if (active.isEmpty()) return false
        var hasChanges = false

        active.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            val remoteOpeningName = openingEntryLinkDao.getRemoteOpeningEntryName(cashbox.localId)
                ?: cashbox.openingEntryId?.takeIf {
                    it.isNotBlank() && !it.startsWith("LOCAL-", ignoreCase = true)
                }
            if (remoteOpeningName.isNullOrBlank()) return@forEach

            val remoteClosing = runCatching {
                api.getPOSClosingEntriesForOpening(remoteOpeningName).firstOrNull()
            }.onFailure {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: remote closing lookup failed for opening $remoteOpeningName",
                    it
                )
            }.getOrNull() ?: return@forEach

            if (remoteClosing.docstatus != null && remoteClosing.docstatus != 1) {
                return@forEach
            }

            val resolvedEndDate =
                remoteClosing.periodEndDate
                    ?: remoteClosing.postingDate
                    ?: cashbox.periodEndDate
                    ?: cashbox.periodStartDate

            ensureRemoteOpeningEntry(remoteOpeningName, cashbox.localId)

            val startMillis = parseErpDateTimeToEpochMillis(cashbox.periodStartDate)
                ?: return@forEach
            val endMillis = parseErpDateTimeToEpochMillis(resolvedEndDate) ?: return@forEach
            val shiftInvoices = cashbox.openingEntryId?.takeIf { it.isNotBlank() }?.let {
                salesInvoiceDao.getInvoicesForOpeningEntry(it)
            } ?: salesInvoiceDao.getInvoicesForShift(
                profileId = cashbox.posProfile,
                startMillis = startMillis,
                endMillis = endMillis
            )

            val dto = buildClosingEntryDto(
                cashbox = cashbox,
                openingEntryId = remoteOpeningName,
                postingDate = resolvedEndDate,
                periodEndDate = resolvedEndDate,
                balanceDetails = wrapper.details,
                invoices = shiftInvoices
            )

            cashboxDao.updateStatus(
                cashbox.localId,
                status = false,
                pceId = remoteClosing.name,
                endDate = resolvedEndDate,
                pendingSync = false
            )
            closingDao.insert(dto.toEntity(remoteClosing.name, pendingSync = false))
            cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, remoteClosing.name)

            if (cashbox.closingEntryId?.startsWith("LOCAL-", ignoreCase = true) == true) {
                closingDao.delete(cashbox.closingEntryId!!)
            }

            if (cashbox.openingEntryId != remoteOpeningName) {
                cashboxDao.updateOpeningEntryId(cashbox.localId, remoteOpeningName)
            }

            hasChanges = true
        }

        return hasChanges
    }

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
            val remoteClosing = runCatching {
                api.getPOSClosingEntriesForOpening(remoteOpeningName).firstOrNull()
            }.onFailure {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: lookup remote closing failed for opening $remoteOpeningName",
                    it
                )
            }.getOrNull()
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

            if (remoteClosing != null) {
                val submitOk = if (remoteClosing.docstatus == 1) {
                    true
                } else {
                    runCatching { api.submitPOSClosingEntry(remoteClosing.name) }
                        .onFailure { AppLogger.warn("ClosingEntrySyncRepository submit failed", it) }
                        .getOrNull() != null
                }
                val pendingSync = !submitOk
                val resolvedEndDate = remoteClosing.periodEndDate ?: periodEnd
                cashboxDao.updateStatus(
                    cashbox.localId,
                    status = false,
                    pceId = remoteClosing.name,
                    endDate = resolvedEndDate,
                    pendingSync = pendingSync
                )
                closingDao.insert(dto.toEntity(remoteClosing.name, pendingSync = pendingSync))
                cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, remoteClosing.name)
                if (localClosingName?.startsWith("LOCAL-", ignoreCase = true) == true) {
                    closingDao.delete(localClosingName)
                }
                hasChanges = true
                return@forEach
            }

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
            cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, response.name)
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
