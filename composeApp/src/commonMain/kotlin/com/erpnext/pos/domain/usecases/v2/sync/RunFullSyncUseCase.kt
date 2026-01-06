package com.erpnext.pos.domain.usecases.v2.sync

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.sync.SyncUnit
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry

data class SyncResult(
    val syncedDoctypes: Set<String>,
    val failedDoctypes: Set<String>
)

class RunSyncUseCase(
    private val syncUnits: List<SyncUnit>
): UseCase<SyncContext, SyncResult>() {

    override suspend fun useCaseFunction(input: SyncContext): SyncResult {
        val synced = mutableSetOf<String>()
        val failed = mutableSetOf<String>()

        syncUnits.forEach { unit ->
            AppSentry.breadcrumb("RunSyncUseCase: ${unit.name} start")
            AppLogger.info("RunSyncUseCase: ${unit.name} start")
            val result = unit.run(input)
            if (result.success) {
                synced += unit.name
                AppSentry.breadcrumb("RunSyncUseCase: ${unit.name} success (changed=${result.changed})")
            } else {
                failed += unit.name
                val error = result.error ?: Exception("Sync unit ${unit.name} failed without error")
                AppSentry.capture(error, "RunSyncUseCase: ${unit.name} failed")
                AppLogger.warn("RunSyncUseCase: ${unit.name} failed", error)
            }
        }

        return SyncResult(
            syncedDoctypes = synced,
            failedDoctypes = failed
        )
    }
}
