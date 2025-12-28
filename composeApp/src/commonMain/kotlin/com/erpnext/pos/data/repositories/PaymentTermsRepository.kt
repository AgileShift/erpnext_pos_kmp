package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.remoteSource.api.APIService

class PaymentTermsRepository(
    private val api: APIService
) {
    suspend fun fetchPaymentTerms(): List<PaymentTermBO> {
        return api.fetchPaymentTerms().map { term ->
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
    }
}
