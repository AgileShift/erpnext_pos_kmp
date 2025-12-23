package com.erpnext.pos.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object TTLCacheValidator {
    @OptIn(ExperimentalTime::class)
    fun isExpired(lastUpdated: Long, ttlMillis: Long): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return (now - lastUpdated) > ttlMillis
    }
}