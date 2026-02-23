package com.erpnext.pos.sync

import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.utils.AppLogger

class PosProfileGate(
    private val syncOrchestrator: SyncOrchestrator,
    private val posProfileLocalDao: PosProfileLocalDao,
    private val posProfilePaymentMethodDao: PosProfilePaymentMethodDao,
    private val posProfileDao: POSProfileDao
) {
    private suspend fun profileNamesWithRelations(profileNames: List<String>): List<String> {
        return profileNames.filter { profileId ->
            posProfilePaymentMethodDao.countRelationsForProfile(profileId) > 0
        }
    }

    private suspend fun enforceProfilesWithRelations(profileNames: List<String>): List<String> {
        val validProfiles = profileNamesWithRelations(profileNames)
        if (validProfiles.size == profileNames.size) return validProfiles

        val missing = profileNames.filterNot { validProfiles.contains(it) }
        AppLogger.warn("PosProfileGate: profiles without payment relations pruned: $missing")
        if (validProfiles.isEmpty()) {
            posProfileLocalDao.softDeleteAll()
            posProfileLocalDao.hardDeleteAllDeleted()
            posProfileDao.softDeleteAll()
            posProfileDao.hardDeleteAllDeleted()
            posProfilePaymentMethodDao.softDeleteAllRelations()
            posProfilePaymentMethodDao.hardDeleteAllDeletedRelations()
            return emptyList()
        }

        posProfileLocalDao.softDeleteNotIn(validProfiles)
        posProfileLocalDao.hardDeleteDeletedNotIn(validProfiles)
        posProfileDao.softDeleteNotIn(validProfiles)
        posProfileDao.hardDeleteDeletedNotIn(validProfiles)
        posProfilePaymentMethodDao.softDeleteForProfilesNotIn(validProfiles)
        posProfilePaymentMethodDao.hardDeleteDeletedForProfilesNotIn(validProfiles)
        return validProfiles
    }

    suspend fun ensureReady(assignedTo: String?): GateResult {
        val cachedProfileNames = posProfileLocalDao.getAll().map { it.profileName }
        if (cachedProfileNames.isNotEmpty()) {
            val validCachedProfiles = enforceProfilesWithRelations(cachedProfileNames)
            if (validCachedProfiles.isNotEmpty()) {
                return GateResult.Ready
            }
        }

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

        val syncedProfileNames = posProfileLocalDao.getAll().map { it.profileName }
        val validSyncedProfiles = enforceProfilesWithRelations(syncedProfileNames)
        return if (validSyncedProfiles.isNotEmpty()) {
            GateResult.Ready
        } else {
            GateResult.Failed("No POS profiles with payment methods after sync")
        }
    }
}
