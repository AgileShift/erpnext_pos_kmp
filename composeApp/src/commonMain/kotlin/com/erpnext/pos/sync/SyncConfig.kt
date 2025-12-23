package com.erpnext.pos.sync

data class SyncConfig(
    val batchSize: Int = 20,
    val initialBackoffMillis: Long = 1000L,
    val backoffFactor: Double = 2.0,
    val maxBackoffMillis: Long = 60_000L
)