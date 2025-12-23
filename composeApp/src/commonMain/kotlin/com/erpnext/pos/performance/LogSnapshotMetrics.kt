package com.erpnext.pos.performance

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LogSnapshotMetrics(
    private val logger: (String) -> Unit
) : SnapshotMetric {

    private val start = Clock.System.now().toEpochMilliseconds()

    override fun mark(section: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        logger("Snapshot[$section] took ${now - start} ms")
    }

    override fun finish() {
        logger("Snapshot[total]: ${Clock.System.now().toEpochMilliseconds() - start}ms")
    }
}