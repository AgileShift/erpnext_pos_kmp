package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanyDto(
    @SerialName("name")
    val company: String,
    @SerialName("default_currency")
    val defaultCurrency: String,
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("default_receivable_account")
    val defaultReceivableAccount: String? = null,
    @SerialName("default_receivable_account_currency")
    val defaultReceivableAccountCurrency: String? = null
)
