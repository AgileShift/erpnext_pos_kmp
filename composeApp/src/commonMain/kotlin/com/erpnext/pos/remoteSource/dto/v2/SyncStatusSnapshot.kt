package com.erpnext.pos.remoteSource.dto.v2

data class SyncStatusSnapshot(
    val lastFullSyncAt: Long?,     // null si nunca se sincronizó
    val pendingInvoices: Int,
    val failedInvoices: Int,
    val isSyncInProgress: Boolean,
    val docTypes: List<SyncDocTypeStateSnapshot> = emptyList()
)
