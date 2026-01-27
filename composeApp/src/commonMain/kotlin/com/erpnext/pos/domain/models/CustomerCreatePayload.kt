package com.erpnext.pos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CustomerCreatePayload(
    val customerName: String,
    val customerType: String,
    val customerGroup: String? = null,
    val territory: String? = null,
    val defaultCurrency: String? = null,
    val defaultPriceList: String? = null,
    val mobileNo: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val taxCategory: String? = null,
    val isInternalCustomer: Boolean = false,
    val representsCompany: String? = null,
    val creditLimit: Double? = null,
    val paymentTerms: String? = null,
    val notes: String? = null,
    val address: Address? = null,
    val contact: Contact? = null
) {
    @Serializable
    data class Address(
        val line1: String? = null,
        val line2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val country: String? = null
    )

    @Serializable
    data class Contact(
        val email: String? = null,
        val mobile: String? = null,
        val phone: String? = null
    )
}
