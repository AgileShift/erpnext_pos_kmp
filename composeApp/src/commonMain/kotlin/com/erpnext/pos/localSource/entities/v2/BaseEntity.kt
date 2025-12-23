package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class BaseEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    @ColumnInfo(name = "created_at")
    var createdAt: Long = Clock.System.now().toEpochMilliseconds()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long? = null

    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false

    @ColumnInfo(name = "companyId")
    var companyId: String = ""

    @ColumnInfo(name = "instanceId")
    var instanceId: String = ""

    @ColumnInfo(name = "lastSyncedAt")
    var lastSyncedAt: Long? = null
}