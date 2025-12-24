package com.erpnext.pos.remoteSource.dto.v2

data class SyncDocTypeStateSnapshot(
    val docType: String,
    val lastPullAt: Long?,
    val lastPushAt: Long?,
    val pendingCount: Int,
    val failedCount: Int,
    val lastError: String?,
    val isSyncInProgress: Boolean
)
