package com.erpnext.pos.sync

import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodSyncRepository
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.NetworkMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncPrerequisite {
    NEED_AUTH,
    NEED_COMPANY,
    NEED_PROFILE_ID,
    NEED_SHIFT_ID
}

enum class SyncJobStatus {
    READY,
    PENDING,
    DONE,
    FAILED
}

data class SyncJobResult(
    val jobId: String,
    val status: SyncJobStatus,
    val message: String? = null
)

class SyncOrchestrator(
    private val networkMonitor: NetworkMonitor,
    private val sessionRefresher: SessionRefresher,
    private val posProfilePaymentMethodSyncRepository: PosProfilePaymentMethodSyncRepository
) {
    private val profileLocks = mutableMapOf<String, Mutex>()

    suspend fun bootstrapOpening(profileId: String): List<SyncJobResult> {
        if (profileId.isBlank()) {
            return listOf(
                SyncJobResult(
                    jobId = "SyncPosProfilePayments",
                    status = SyncJobStatus.FAILED,
                    message = "Missing profileId"
                )
            )
        }
        val lock = profileLocks.getOrPut(profileId) { Mutex() }
        return lock.withLock {
            val prereqResults = mutableListOf<SyncJobResult>()
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                prereqResults.add(
                    SyncJobResult(
                        jobId = "bootstrapOpening",
                        status = SyncJobStatus.PENDING,
                        message = "Offline"
                    )
                )
                return@withLock prereqResults
            }
            if (!sessionRefresher.ensureValidSession()) {
                prereqResults.add(
                    SyncJobResult(
                        jobId = "bootstrapOpening",
                        status = SyncJobStatus.FAILED,
                        message = "Invalid session"
                    )
                )
                return@withLock prereqResults
            }
            val results = mutableListOf<SyncJobResult>()
            results.add(
                runJob("SyncPosProfilePayments", setOf(SyncPrerequisite.NEED_AUTH, SyncPrerequisite.NEED_PROFILE_ID)) {
                    posProfilePaymentMethodSyncRepository.syncProfilePayments(profileId)
                }
            )
            return@withLock results
        }
    }

    suspend fun bootstrapProfiles(assignedTo: String?): List<SyncJobResult> {
        val lockKey = assignedTo ?: "all"
        val lock = profileLocks.getOrPut(lockKey) { Mutex() }
        return lock.withLock {
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                return@withLock listOf(
                    SyncJobResult(
                        jobId = "SyncPosProfiles",
                        status = SyncJobStatus.PENDING,
                        message = "Offline"
                    )
                )
            }
            if (!sessionRefresher.ensureValidSession()) {
                return@withLock listOf(
                    SyncJobResult(
                        jobId = "SyncPosProfiles",
                        status = SyncJobStatus.FAILED,
                        message = "Invalid session"
                    )
                )
            }
            return@withLock listOf(
                runJob("SyncPosProfiles", setOf(SyncPrerequisite.NEED_AUTH)) {
                    posProfilePaymentMethodSyncRepository.syncProfiles(assignedTo)
                }
            )
        }
    }

    private suspend fun runJob(
        jobId: String,
        prereqs: Set<SyncPrerequisite>,
        action: suspend () -> Unit
    ): SyncJobResult {
        AppLogger.info("SyncOrchestrator: running job $jobId prereqs=$prereqs")
        return runCatching {
            action()
            SyncJobResult(jobId, SyncJobStatus.DONE)
        }.getOrElse { error ->
            AppSentry.capture(error, "SyncOrchestrator job $jobId failed")
            AppLogger.warn("SyncOrchestrator job $jobId failed", error)
            SyncJobResult(jobId, SyncJobStatus.FAILED, error.message)
        }
    }
}
