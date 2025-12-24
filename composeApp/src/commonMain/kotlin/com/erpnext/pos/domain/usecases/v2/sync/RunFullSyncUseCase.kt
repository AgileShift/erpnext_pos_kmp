package com.erpnext.pos.domain.usecases.v2.sync

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.sync.SyncUnit
import com.erpnext.pos.domain.usecases.UseCase

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
            val result = unit.run(input)
            if (result.success) {
                synced += unit.name
            } else {
                failed += unit.name
            }
        }

        return SyncResult(
            syncedDoctypes = synced,
            failedDoctypes = failed
        )
    }
}
