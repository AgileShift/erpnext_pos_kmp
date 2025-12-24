package com.erpnext.pos.domain.sync

data class SyncUnitResult(
    val success: Boolean,
    val changed: Boolean,
    val error: Throwable? = null
)

data class PendingSync<T>(
    val localId: String,
    val payload: T
)

interface SyncUnit {
    val name: String
    suspend fun run(ctx: SyncContext): SyncUnitResult
}

data class SyncContext(
    val instanceId: String,  // local only
    val companyId: String,
    val territoryId: String,
    val warehouseId: String,
    val priceList: String,
    val fromDate: String // yyyy-MM-dd
)
