package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabCategory")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = false)
    var name: String,
    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    var lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
