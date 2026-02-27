package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.buildOpeningEntryDto
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.PendingOpeningEntrySync
import com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto
import com.erpnext.pos.utils.AppLogger

class OpeningEntrySyncRepository(
    private val posOpeningRepository: PosOpeningRepository,
    private val openingEntryDao: POSOpeningEntryDao,
    private val openingEntryLinkDao: POSOpeningEntryLinkDao,
    private val cashboxDao: CashboxDao,
    private val salesInvoiceDao: SalesInvoiceDao
) {
    suspend fun pushPending(): Boolean {
        repairActiveOpenings()
        val pending = openingEntryLinkDao.getPendingSync()
        if (pending.isEmpty()) return false
        var hasChanges = false

        pending.forEach { candidate ->
            val dto = buildOpeningEntryDto(candidate.openingEntry, candidate.balanceDetails)
            runCatching {
                val remoteName = candidate.link.remoteOpeningEntryName ?: run {
                    val response = posOpeningRepository.createOpeningEntry(dto)
                    openingEntryLinkDao.updateRemoteName(candidate.link.id, response.name)
                    cashboxDao.updateOpeningEntryId(candidate.cashbox.localId, response.name)
                    response.name
                }
                ensureRemoteOpeningEntry(remoteName, candidate.openingEntry)
                updateOpeningEntryRefs(candidate.openingEntry.name, remoteName)
                openingEntryLinkDao.markSynced(candidate.link.id, remoteName)
                openingEntryDao.update(candidate.openingEntry.copy(pendingSync = false))
                cashboxDao.updatePendingSync(candidate.cashbox.localId, false)
                hasChanges = true
            }.onFailure { error ->
                val remote = resolveRemoteOpenSession(candidate)
                if (remote != null && remote.name.isNotBlank()) {
                    val remoteName = remote.name
                    openingEntryLinkDao.updateRemoteName(candidate.link.id, remoteName)
                    cashboxDao.updateOpeningEntryId(candidate.cashbox.localId, remoteName)
                    ensureRemoteOpeningEntry(remoteName, candidate.openingEntry)
                    updateOpeningEntryRefs(candidate.openingEntry.name, remoteName)
                    openingEntryLinkDao.markSynced(candidate.link.id, remoteName)
                    openingEntryDao.update(candidate.openingEntry.copy(pendingSync = false))
                    cashboxDao.updatePendingSync(candidate.cashbox.localId, false)
                    hasChanges = true
                    AppLogger.info(
                        "OpeningEntrySyncRepository: adopted existing remote opening $remoteName " +
                            "after local push failure"
                    )
                    return@onFailure
                }
                if (isCashierAssignedError(error)) {
                    AppLogger.warn(
                        "OpeningEntrySyncRepository: cashier-assigned conflict without resolvable remote opening",
                        error
                    )
                    return@onFailure
                }
                AppLogger.warn("OpeningEntrySyncRepository pushPending failed", error)
            }
        }

        return hasChanges
    }

    suspend fun repairActiveOpenings() {
        val activeCashboxes = cashboxDao.getActiveCashboxes()
        activeCashboxes.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            val link = openingEntryLinkDao.getByCashboxId(cashbox.localId)
            val localOpeningName = wrapper.details.firstOrNull()?.posOpeningEntry
                ?: cashbox.openingEntryId?.takeIf { it.startsWith("LOCAL-", ignoreCase = true) }
            if (localOpeningName.isNullOrBlank()) return@forEach

            val openingEntry = openingEntryDao.getByName(localOpeningName)
            if (openingEntry == null) {
                AppLogger.warn(
                    "OpeningEntrySyncRepository: missing opening entry for cashbox ${cashbox.localId}"
                )
                return@forEach
            }

            val cashboxRemote = cashbox.openingEntryId
                ?.takeIf { !it.startsWith("LOCAL-", ignoreCase = true) }
            val linkRemote = link?.remoteOpeningEntryName
            val remoteName = cashboxRemote ?: linkRemote

            if (link == null) {
                val pendingSync = remoteName.isNullOrBlank() || openingEntry.pendingSync ||
                        cashbox.pendingSync
                openingEntryLinkDao.insert(
                    com.erpnext.pos.localSource.entities.POSOpeningEntryLinkEntity(
                        cashboxId = cashbox.localId,
                        localOpeningEntryName = openingEntry.name,
                        remoteOpeningEntryName = remoteName,
                        pendingSync = pendingSync
                    )
                )
            }

            if (!remoteName.isNullOrBlank()) {
                ensureRemoteOpeningEntry(remoteName, openingEntry)
                updateOpeningEntryRefs(openingEntry.name, remoteName)
                if (cashbox.openingEntryId != remoteName) {
                    cashboxDao.updateOpeningEntryId(cashbox.localId, remoteName)
                }
                if (openingEntry.pendingSync) {
                    openingEntryDao.update(openingEntry.copy(pendingSync = false))
                }
                if (cashbox.pendingSync) {
                    cashboxDao.updatePendingSync(cashbox.localId, false)
                }
                if (link != null && link.pendingSync) {
                    openingEntryLinkDao.markSynced(link.id, remoteName)
                }
            }
        }
    }

    private suspend fun ensureRemoteOpeningEntry(
        remoteName: String,
        local: com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
    ) {
        val existing = openingEntryDao.getByName(remoteName)
        if (existing != null) return
        openingEntryDao.insert(
            local.copy(name = remoteName, pendingSync = false)
        )
    }

    private suspend fun updateOpeningEntryRefs(localName: String, remoteName: String) {
        if (localName == remoteName) return
        cashboxDao.updateBalanceDetailsOpeningEntry(localName, remoteName)
        val affected = salesInvoiceDao.getInvoicesForOpeningEntry(localName)
        salesInvoiceDao.updateInvoicesOpeningEntry(localName, remoteName)
        salesInvoiceDao.updatePaymentsOpeningEntry(localName, remoteName)
        if (affected.isEmpty()) return
        val remoteSyncedAffected = affected.count { invoice ->
            val invoiceName = invoice.invoiceName?.trim().orEmpty()
            invoiceName.isNotBlank() &&
                !invoiceName.startsWith("LOCAL-", ignoreCase = true) &&
                invoice.syncStatus.equals("Synced", ignoreCase = true)
        }
        if (remoteSyncedAffected > 0) {
            AppLogger.info(
                "OpeningEntrySyncRepository: omitiendo update remoto de $remoteSyncedAffected factura(s); " +
                    "updateSalesInvoice fue removido y la reconciliacion se hace via API v1 + bootstrap."
            )
        }
    }

    private fun isCashierAssignedError(error: Throwable): Boolean {
        val frappe = error as? com.erpnext.pos.remoteSource.sdk.FrappeException
        val messages = buildList {
            error.message?.let { add(it) }
            frappe?.errorResponse?._server_messages?.let { add(it) }
            frappe?.errorResponse?.exception?.let { add(it) }
        }.map { it.lowercase() }
        return messages.any { msg ->
            msg.contains("cashier") && msg.contains("assigned") && msg.contains("pos")
        }
    }

    private suspend fun resolveRemoteOpenSession(candidate: PendingOpeningEntrySync): POSOpeningEntrySummaryDto? {
        val user = candidate.openingEntry.user
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: candidate.cashbox.user.trim().takeIf { it.isNotBlank() }
        val profile = candidate.openingEntry.posProfile
        if (user.isNullOrBlank() || profile.isBlank()) return null
        return runCatching {
            posOpeningRepository.getOpenSession(user, profile)
        }.getOrNull()
    }
}
