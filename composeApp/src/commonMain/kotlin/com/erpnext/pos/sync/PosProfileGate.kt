package com.erpnext.pos.sync

import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.utils.AppLogger

class PosProfileGate(
    private val syncOrchestrator: SyncOrchestrator,
    private val posProfileLocalDao: PosProfileLocalDao
) {
    suspend fun ensureReady(assignedTo: String?): GateResult {
        val existing = posProfileLocalDao.countAll()
        if (existing > 0) return GateResult.Ready

        AppLogger.info("PosProfileGate: cache miss, bootstrapping profiles")
        val results = syncOrchestrator.bootstrapProfiles(assignedTo)
        val hasFailure = results.any { it.status == SyncJobStatus.FAILED }
        if (hasFailure) {
            val message = results.firstOrNull { it.status == SyncJobStatus.FAILED }?.message
                ?: "Sync failed"
            return GateResult.Failed(message)
        }
        val hasPending = results.any { it.status == SyncJobStatus.PENDING }
        if (hasPending) {
            val message = results.firstOrNull { it.status == SyncJobStatus.PENDING }?.message
                ?: "Sync pending"
            return GateResult.Pending(message)
        }
        return if (posProfileLocalDao.countAll() > 0) {
            GateResult.Ready
        } else {
            GateResult.Failed("No profiles found after sync")
        }
    }
}
