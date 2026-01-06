package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.RepoTrace

class PaymentTermsRepository(
    private val api: APIService
) {
    suspend fun fetchPaymentTerms(): List<PaymentTermBO> {
        RepoTrace.breadcrumb("PaymentTermsRepository", "fetchPaymentTerms")
        return runCatching {
            api.fetchPaymentTerms().map { term ->
                PaymentTermBO(
                    name = term.paymentTermName,
                    invoicePortion = term.invoicePortion,
                    modeOfPayment = term.modeOfPayment,
                    dueDateBasedOn = term.dueDateBasedOn,
                    creditDays = term.creditDays,
                    creditMonths = term.creditMonths,
                    discountType = term.discountType,
                    discount = term.discount,
                    description = term.description,
                    discountValidity = term.discountValidity,
                    discountValidityBasedOn = term.discountValidityBasedOn
                )
            }
        }.getOrElse {
            RepoTrace.capture("PaymentTermsRepository", "fetchPaymentTerms", it)
            throw it
        }
    }
}
