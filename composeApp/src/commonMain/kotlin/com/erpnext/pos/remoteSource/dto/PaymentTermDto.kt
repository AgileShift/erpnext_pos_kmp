package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentTermDto(
    @SerialName("payment_term_name")
    val paymentTermName: String,
    @SerialName("invoice_portion")
    val invoicePortion: Double? = null,
    @SerialName("mode_of_payment")
    val modeOfPayment: String? = null,
    @SerialName("due_date_based_on")
    val dueDateBasedOn: String? = null,
    @SerialName("credit_days")
    val creditDays: Int? = null,
    @SerialName("credit_months")
    val creditMonths: Int? = null,
    @SerialName("discount_type")
    val discountType: String? = null,
    @SerialName("discount")
    val discount: Double? = null,
    val description: String? = null,
    @SerialName("discount_validity")
    val discountValidity: Int? = null,
    @SerialName("discount_validity_based_on")
    val discountValidityBasedOn: String? = null
)
