package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyDto(
    val name: String,
    @SerialName("currency_name") val currencyName: String? = null,
    val symbol: String? = null,
    @SerialName("number_format") val numberFormat: String? = null
)

@Serializable
data class ModeOfPaymentDto(
    val name: String,
    @SerialName("mode_of_payment") val modeOfPayment: String,
    val currency: String? = null,
    @Serializable(with = IntAsBooleanSerializer::class)
    val enabled: Boolean = true
)

@Serializable
data class ModeOfPaymentDetailDto(
    val name: String,
    @SerialName("mode_of_payment") val modeOfPayment: String,
    @Serializable(with = IntAsBooleanSerializer::class)
    val enabled: Boolean = true,
    val type: String? = null,
    @SerialName("accounts")
    val accounts: List<ModeOfPaymentAccountDto> = emptyList()
)

@Serializable
data class ModeOfPaymentAccountDto(
    val company: String,
    @SerialName("default_account") val defaultAccount: String? = null
)

@Serializable
data class AccountDetailDto(
    val name: String,
    @SerialName("account_currency") val accountCurrency: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    val company: String? = null
)
