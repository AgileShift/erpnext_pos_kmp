package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "sync_state",
    primaryKeys = ["instanceId", "companyId", "docType"]
)
data class SyncStateEntity(
    @androidx.room.ColumnInfo(defaultValue = "")
    val docType: String,
    val lastFullSyncAt: Long?,
    @androidx.room.ColumnInfo(name = "pendingInvoices", defaultValue = "0")
    val pendingCount: Int,
    @androidx.room.ColumnInfo(name = "failedInvoices", defaultValue = "0")
    val failedCount: Int,
    @androidx.room.ColumnInfo(defaultValue = "NULL")
    val lastPullAt: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL")
    val lastPushAt: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL")
    val lastError: String? = null,
    val isSyncInProgress: Boolean = false
) : BaseEntity()
