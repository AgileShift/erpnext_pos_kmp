package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.TerritoryBO
import com.erpnext.pos.localSource.datasources.TerritoryLocalSource
import com.erpnext.pos.utils.RepoTrace

class TerritoryRepository(
    private val localSource: TerritoryLocalSource
) {
    suspend fun getLocalTerritories(): List<TerritoryBO> {
        RepoTrace.breadcrumb("TerritoryRepository", "getLocalTerritories")
        return localSource.getAll().map { it.toBO() }
    }
}
