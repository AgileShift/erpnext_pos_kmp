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
