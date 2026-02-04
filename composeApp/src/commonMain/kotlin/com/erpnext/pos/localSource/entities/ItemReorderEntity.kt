package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "item_reorders",
    primaryKeys = ["instanceId", "companyId", "itemId", "warehouseId"]
)
data class ItemReorderEntity(
    val itemId: String,
    val warehouseId: String,
    val reorderLevel: Double? = null,
    val reorderQty: Double? = null,
    val id: Long = 0L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    val companyId: String,
    val instanceId: String,
    val lastSyncedAt: Long? = null
)
