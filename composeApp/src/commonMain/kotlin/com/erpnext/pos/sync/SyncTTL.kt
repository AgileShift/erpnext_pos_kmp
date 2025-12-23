package com.erpnext.pos.sync

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object SyncTTL {
    private const val TTL_HOURS = 6

    fun isExpired(lastSyncedAt: Long?): Boolean {
        if (lastSyncedAt == null) return true
        val elapsedHours =
            (Clock.System.now().toEpochMilliseconds() - lastSyncedAt) / (1000 * 60 * 60)
        return elapsedHours >= TTL_HOURS
    }
}