package com.erpnext.pos.sync

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object SyncTTL {
    const val DEFAULT_TTL_HOURS = 6

    fun isExpired(lastSyncedAt: Long?, ttlHours: Int = DEFAULT_TTL_HOURS): Boolean {
        if (lastSyncedAt == null) return true
        val elapsedHours =
            (Clock.System.now().toEpochMilliseconds() - lastSyncedAt) / (1000 * 60 * 60)
        return elapsedHours >= ttlHours
    }
}
