package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.localSource.datasources.PaymentTermLocalSource
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import io.ktor.client.plugins.ClientRequestException

class PaymentTermsRepository(
    private val api: APIService,
    private val localSource: PaymentTermLocalSource
) {
    suspend fun getLocalPaymentTerms(): List<PaymentTermBO> {
        RepoTrace.breadcrumb("PaymentTermsRepository", "getLocalPaymentTerms")
        return localSource.getAll().map { it.toBO() }
    }

    suspend fun fetchPaymentTerms(): List<PaymentTermBO> {
        RepoTrace.breadcrumb("PaymentTermsRepository", "fetchPaymentTerms")

        val localData = localSource.getAll().map { it.toBO() }

        return runCatching { api.fetchPaymentTerms() }
            .onSuccess { terms ->
                val entities = terms.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    localSource.insertAll(entities)
                }
                val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
                localSource.hardDeleteDeletedMissing(names)
                localSource.softDeleteMissing(names)
            }
            .map { terms -> terms.map { it.toEntity().toBO() } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.hardDeleteAllDeleted()
                    localSource.softDeleteAll()
                    return emptyList()
                }
                RepoTrace.capture("PaymentTermsRepository", "fetchPaymentTerms", error)
                localData
            }
    }
}
