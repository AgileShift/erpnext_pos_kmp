package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.TerritoryBO
import com.erpnext.pos.localSource.datasources.TerritoryLocalSource
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import io.ktor.client.plugins.ClientRequestException

class TerritoryRepository(
    private val api: APIService,
    private val localSource: TerritoryLocalSource
) {
    suspend fun getLocalTerritories(): List<TerritoryBO> {
        RepoTrace.breadcrumb("TerritoryRepository", "getLocalTerritories")
        return localSource.getAll().map { it.toBO() }
    }

    suspend fun fetchTerritories(): List<TerritoryBO> {
        RepoTrace.breadcrumb("TerritoryRepository", "fetchTerritories")

        val localData = localSource.getAll().map { it.toBO() }

        return runCatching { api.fetchTerritories() }
            .onSuccess { territories ->
                val entities = territories.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    localSource.insertAll(entities)
                }
                val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
                localSource.hardDeleteDeletedMissing(names)
                localSource.softDeleteMissing(names)
            }
            .map { territories -> territories.map { it.toEntity().toBO() } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.hardDeleteAllDeleted()
                    localSource.softDeleteAll()
                    return emptyList()
                }
                RepoTrace.capture("TerritoryRepository", "fetchTerritories", error)
                localData
            }
    }
}
