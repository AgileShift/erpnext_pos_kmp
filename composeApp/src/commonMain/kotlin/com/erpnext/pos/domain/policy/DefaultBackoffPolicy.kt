package com.erpnext.pos.domain.policy

import kotlin.math.min
import kotlin.math.pow

class DefaultBackoffPolicy(
    private val baseDelayMs: Long = 30_000L,      // 30s
    private val maxDelayMs: Long = 15 * 60_000L,  // 15 min
    private val jitterRatio: Double = 0.2         // ±20%
) : BackoffPolicy {

    override fun nextDelayMs(attempt: Int, randomFactor: Double): Long {
        require(randomFactor in 0.0..1.0) {
            "randomFactor must be between 0.0 and 1.0"
        }

        // Exponential backoff
        val exponential =
            baseDelayMs * 2.0.pow(attempt.toDouble())

        val capped = min(exponential.toLong(), maxDelayMs)

        // Jitter: +/- jitterRatio
        val jitter =
            (capped * jitterRatio * (randomFactor * 2 - 1)).toLong()

        return (capped + jitter).coerceAtLeast(0L)
    }
}
