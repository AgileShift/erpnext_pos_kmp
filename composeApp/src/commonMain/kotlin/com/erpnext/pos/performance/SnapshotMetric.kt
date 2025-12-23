package com.erpnext.pos.performance

interface SnapshotMetric {
    fun mark(section: String)
    fun finish()
}