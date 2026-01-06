package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabDeliveryCharge")
data class DeliveryChargeEntity(
    @PrimaryKey
    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "default_rate")
    val defaultRate: Double,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
