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
    suspend fun fetchPaymentTerms(): List<PaymentTermBO> {
        RepoTrace.breadcrumb("PaymentTermsRepository", "fetchPaymentTerms")

        val localData = localSource.getAll().map { it.toBO() }

        return runCatching { api.fetchPaymentTerms() }
            .onSuccess { terms ->
                localSource.deleteAll()
                if (terms.isNotEmpty()) {
                    localSource.insertAll(terms.map { it.toEntity() })
                }
            }
            .map { terms -> terms.map { it.toEntity().toBO() } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.deleteAll()
                    return emptyList()
                }
                RepoTrace.capture("PaymentTermsRepository", "fetchPaymentTerms", error)
                localData
            }
    }
}
