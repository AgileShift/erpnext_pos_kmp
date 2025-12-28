package com.erpnext.pos.domain.models

data class PaymentTermBO(
    val name: String,
    val invoicePortion: Double? = null,
    val modeOfPayment: String? = null,
    val dueDateBasedOn: String? = null,
    val creditDays: Int? = null,
    val creditMonths: Int? = null,
    val discountType: String? = null,
    val discount: Double? = null,
    val description: String? = null,
    val discountValidity: Int? = null,
    val discountValidityBasedOn: String? = null
)
