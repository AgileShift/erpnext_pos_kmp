package com.erpnext.pos.sync

import com.erpnext.pos.domain.sync.SyncContext

data class PushSyncConflict(
    val docType: String,
    val localId: String,
    val remoteId: String? = null,
    val reason: String
)

data class PushQueueReport(
    val hasChanges: Boolean = false,
    val conflicts: List<PushSyncConflict> = emptyList()
) {
    val hasConflicts: Boolean get() = conflicts.isNotEmpty()
    val conflictCount: Int get() = conflicts.size

    fun merge(other: PushQueueReport): PushQueueReport {
        return PushQueueReport(
            hasChanges = hasChanges || other.hasChanges,
            conflicts = conflicts + other.conflicts
        )
    }

    companion object {
        val EMPTY = PushQueueReport()
    }
}

interface PushSyncRunner {
    suspend fun runPushQueue(ctx: SyncContext, onDocType: (String) -> Unit): PushQueueReport
}
