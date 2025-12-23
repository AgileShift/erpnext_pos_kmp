package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "sync_state",
    primaryKeys = ["instanceId", "companyId"]
)
data class SyncStateEntity(
    val lastFullSyncAt: Long?,
    val pendingInvoices: Int,
    val failedInvoices: Int,

    val isSyncInProgress: Boolean = false
) : BaseEntity()