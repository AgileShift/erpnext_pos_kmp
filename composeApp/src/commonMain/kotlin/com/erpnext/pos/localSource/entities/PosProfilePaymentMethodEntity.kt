package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "tabPOSProfilePaymentMethod",
    primaryKeys = ["profile_id", "mop_name"]
)
data class PosProfilePaymentMethodEntity(
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "mop_name")
    val mopName: String,
    @ColumnInfo(name = "idx")
    val idx: Int,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "allow_in_returns")
    val allowInReturns: Boolean,
    @ColumnInfo(name = "enabled_in_profile")
    val enabledInProfile: Boolean = true,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
