package com.erpnext.pos.sync

import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.utils.AppLogger

sealed class GateResult {
    data object Ready : GateResult()
    data class Pending(val reason: String) : GateResult()
    data class Failed(val reason: String) : GateResult()
}

class OpeningGate(
    private val syncOrchestrator: SyncOrchestrator,
    private val localRepository: PosProfilePaymentMethodLocalRepository
) {
    suspend fun ensureReady(profileId: String): GateResult {
        if (profileId.isBlank()) return GateResult.Failed("Missing profileId")
        if (localRepository.hasResolvedMethods(profileId)) return GateResult.Ready

        AppLogger.info("OpeningGate: cache miss, bootstrapping profile $profileId")
        val results = syncOrchestrator.bootstrapOpening(profileId)
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
        return if (localRepository.hasResolvedMethods(profileId)) {
            GateResult.Ready
        } else {
            GateResult.Failed("No payment methods resolved after sync")
        }
    }
}
