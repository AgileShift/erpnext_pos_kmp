package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.PaymentOutCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentOutSubmitDto
import com.erpnext.pos.remoteSource.dto.PurchaseInvoiceOutstandingDto
import com.erpnext.pos.utils.RepoTrace

class PaymentOutRepository(
    private val api: APIService
) {
    suspend fun createPaymentOut(clientRequestId: String, payload: PaymentOutCreateDto): PaymentOutSubmitDto {
        RepoTrace.breadcrumb("PaymentOutRepository", "createPaymentOut")
        return runCatching {
            api.createPaymentOut(clientRequestId, payload)
        }.getOrElse {
            RepoTrace.capture("PaymentOutRepository", "createPaymentOut", it)
            throw it
        }
    }

    suspend fun fetchOutstandingPurchaseInvoicesForSupplier(supplier: String): List<PurchaseInvoiceOutstandingDto> {
        RepoTrace.breadcrumb("PaymentOutRepository", "fetchOutstandingPurchaseInvoicesForSupplier")
        return runCatching {
            api.fetchOutstandingPurchaseInvoicesForSupplier(supplier)
        }.getOrElse {
            RepoTrace.capture("PaymentOutRepository", "fetchOutstandingPurchaseInvoicesForSupplier", it)
            throw it
        }
    }
}
