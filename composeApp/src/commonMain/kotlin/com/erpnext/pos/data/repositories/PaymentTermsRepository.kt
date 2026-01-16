package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.localSource.datasources.PaymentTermLocalSource
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace

class PaymentTermsRepository(
    private val api: APIService,
    private val localSource: PaymentTermLocalSource
) {
    suspend fun fetchPaymentTerms(): List<PaymentTermBO> {
        RepoTrace.breadcrumb("PaymentTermsRepository", "fetchPaymentTerms")

        val localData = localSource.getAll().map { it.toBO() }
        if (localData.isNotEmpty()) return localData

        val remote = runCatching { api.fetchPaymentTerms() }
            .onSuccess { terms ->
                if (terms.isNotEmpty()) {
                    localSource.insertAll(terms.map { it.toEntity() })
                }
            }
            .getOrElse { error ->
                RepoTrace.capture("PaymentTermsRepository", "fetchPaymentTerms", error)
                emptyList()
            }

        return remote.map { term -> term.toEntity().toBO() }
            .ifEmpty { emptyList() }
    }
}
