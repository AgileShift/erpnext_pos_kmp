package com.erpnext.pos.localSource.dao

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}

data class InstanceScope(
    val instanceId: String,
    val companyId: String
)