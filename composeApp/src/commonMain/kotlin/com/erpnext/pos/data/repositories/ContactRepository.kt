package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.datasources.ContactLocalSource
import com.erpnext.pos.localSource.entities.ContactEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import io.ktor.client.plugins.ClientRequestException

class ContactRepository(
    private val api: APIService,
    private val localSource: ContactLocalSource
) {
    suspend fun getLocalContacts(): List<ContactEntity> = localSource.getAll()

    suspend fun fetchCustomerContacts(): List<ContactEntity> {
        RepoTrace.breadcrumb("ContactRepository", "fetchCustomerContacts")
        val localData = localSource.getAll()
        return runCatching { api.fetchCustomerContacts() }
            .onSuccess { contacts ->
                val entities = contacts.map { it.toEntity() }.filter { it.customerId != null }
                if (entities.isNotEmpty()) {
                    localSource.insertAll(entities)
                }
                val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
                localSource.hardDeleteDeletedMissing(names)
                localSource.softDeleteMissing(names)
            }
            .map { contacts -> contacts.map { it.toEntity() }.filter { it.customerId != null } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.hardDeleteAllDeleted()
                    localSource.softDeleteAll()
                    return emptyList()
                }
                RepoTrace.capture("ContactRepository", "fetchCustomerContacts", error)
                localData
            }
    }
}
