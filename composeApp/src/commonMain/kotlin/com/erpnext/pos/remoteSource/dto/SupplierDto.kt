package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupplierDto(
    val name: String,
    @SerialName("supplier_name")
    val supplierName: String? = null,
    @SerialName("supplier_group")
    val supplierGroup: String? = null,
    @SerialName("supplier_type")
    val supplierType: String? = null,
    @SerialName("default_currency")
    val defaultCurrency: String? = null,
    @SerialName("payment_terms")
    val paymentTerms: String? = null,
    @SerialName("disabled")
    val disabled: Int? = null
)

@Serializable
data class CompanyAccountDto(
    val name: String,
    @SerialName("account_name")
    val accountName: String? = null,
    @SerialName("account_type")
    val accountType: String? = null,
    @SerialName("account_currency")
    val accountCurrency: String? = null,
    val company: String? = null,
    @SerialName("is_group")
    val isGroup: Int? = null,
    val disabled: Int? = null
)
