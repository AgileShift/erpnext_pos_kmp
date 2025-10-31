package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
open class BaseEntity() {
    @ColumnInfo(name = "created_at")
    var createdAt: Long = Clock.System.now().toEpochMilliseconds()

    @ColumnInfo(name = "synced")
    var synced: Boolean = false

    @ColumnInfo(name = "last_attempt")
    var lastAttempt: Long? = null

    @ColumnInfo(name = "attempts")
    var attempts: Int = 0
}
