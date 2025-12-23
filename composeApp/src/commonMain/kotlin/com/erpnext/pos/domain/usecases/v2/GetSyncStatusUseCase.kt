package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.remoteSource.dto.v2.SyncStatusSnapshot

data class SyncStatusInput(
    val instanceId: String,
    val companyId: String
)

class GetSyncStatusUseCase(
    private val syncRepository: SyncRepository
) : UseCase<SyncStatusInput, SyncStatusSnapshot>() {
    override suspend fun useCaseFunction(input: SyncStatusInput): SyncStatusSnapshot {
        return syncRepository.getOrCreate(input.instanceId, input.companyId)
    }
}