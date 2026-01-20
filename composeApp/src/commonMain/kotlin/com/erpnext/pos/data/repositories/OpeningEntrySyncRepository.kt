package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.buildOpeningEntryDto
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.utils.AppLogger

class OpeningEntrySyncRepository(
    private val posOpeningRepository: PosOpeningRepository,
    private val openingEntryDao: POSOpeningEntryDao,
    private val openingEntryLinkDao: POSOpeningEntryLinkDao,
    private val cashboxDao: CashboxDao
) {
    suspend fun pushPending(): Boolean {
        val pending = openingEntryLinkDao.getPendingSync()
        if (pending.isEmpty()) return false
        var hasChanges = false

        pending.forEach { candidate ->
            val dto = buildOpeningEntryDto(candidate.openingEntry, candidate.balanceDetails)
            runCatching {
                val response = posOpeningRepository.createOpeningEntry(dto)
                posOpeningRepository.submitOpeningEntry(response.name)
                openingEntryLinkDao.markSynced(candidate.link.id, response.name)
                openingEntryDao.update(candidate.openingEntry.copy(pendingSync = false))
                cashboxDao.updateOpeningEntryId(candidate.cashbox.localId, response.name)
                hasChanges = true
            }.onFailure { error ->
                AppLogger.warn("OpeningEntrySyncRepository pushPending failed", error)
            }
        }

        return hasChanges
    }
}
