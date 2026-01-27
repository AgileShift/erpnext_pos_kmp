package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(tableName = "tabPosProfileLocal")
data class PosProfileLocalEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "profile_name")
    val profileName: String,
    @ColumnInfo(name = "company")
    val company: String,
    @ColumnInfo(name = "currency")
    val currency: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
