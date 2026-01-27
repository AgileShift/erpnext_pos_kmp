package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.datasources.AddressLocalSource
import com.erpnext.pos.localSource.entities.AddressEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import io.ktor.client.plugins.ClientRequestException

class AddressRepository(
    private val api: APIService,
    private val localSource: AddressLocalSource
) {
    suspend fun getLocalAddresses(): List<AddressEntity> = localSource.getAll()

    suspend fun fetchCustomerAddresses(): List<AddressEntity> {
        RepoTrace.breadcrumb("AddressRepository", "fetchCustomerAddresses")
        val localData = localSource.getAll()
        return runCatching { api.fetchCustomerAddresses() }
            .onSuccess { addresses ->
                val entities = addresses.map { it.toEntity() }.filter { it.customerId != null }
                if (entities.isNotEmpty()) {
                    localSource.insertAll(entities)
                }
                val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
                localSource.hardDeleteDeletedMissing(names)
                localSource.softDeleteMissing(names)
            }
            .map { addresses -> addresses.map { it.toEntity() }.filter { it.customerId != null } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.hardDeleteAllDeleted()
                    localSource.softDeleteAll()
                    return emptyList()
                }
                RepoTrace.capture("AddressRepository", "fetchCustomerAddresses", error)
                localData
            }
    }
}
