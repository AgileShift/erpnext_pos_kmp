package com.erpnext.pos.domain.repositories.v2

import com.erpnext.pos.localSource.entities.v2.SyncStateEntity
import com.erpnext.pos.remoteSource.dto.v2.SyncStatusSnapshot

interface ISyncRepository {
    suspend fun getOrCreate(instanceId: String, companyId: String): SyncStatusSnapshot?
    suspend fun update(entity: SyncStateEntity)
}