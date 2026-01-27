package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.CustomerGroupBO
import com.erpnext.pos.localSource.datasources.CustomerGroupLocalSource
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import io.ktor.client.plugins.ClientRequestException

class CustomerGroupRepository(
    private val api: APIService,
    private val localSource: CustomerGroupLocalSource
) {
    suspend fun getLocalCustomerGroups(): List<CustomerGroupBO> {
        RepoTrace.breadcrumb("CustomerGroupRepository", "getLocalCustomerGroups")
        return localSource.getAll().map { it.toBO() }
    }

    suspend fun fetchCustomerGroups(): List<CustomerGroupBO> {
        RepoTrace.breadcrumb("CustomerGroupRepository", "fetchCustomerGroups")

        val localData = localSource.getAll().map { it.toBO() }

        return runCatching { api.fetchCustomerGroups() }
            .onSuccess { groups ->
                val entities = groups.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    localSource.insertAll(entities)
                }
                val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
                localSource.hardDeleteDeletedMissing(names)
                localSource.softDeleteMissing(names)
            }
            .map { groups -> groups.map { it.toEntity().toBO() } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.hardDeleteAllDeleted()
                    localSource.softDeleteAll()
                    return emptyList()
                }
                RepoTrace.capture("CustomerGroupRepository", "fetchCustomerGroups", error)
                localData
            }
    }
}
