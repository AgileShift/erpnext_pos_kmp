package com.erpnext.pos.domain.models

data class POSCurrencyOption(
    val code: String,
    val name: String,
    val symbol: String? = null,
    val numberFormat: String? = null
)

data class POSPaymentModeOption(
    val name: String,
    val modeOfPayment: String,
    val account: String? = null,
    val currency: String? = null,
    val type: String? = null,
    val allowInReturns: Boolean = true,
)
