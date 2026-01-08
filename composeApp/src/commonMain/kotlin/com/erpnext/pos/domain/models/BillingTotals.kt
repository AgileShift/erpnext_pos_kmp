package com.erpnext.pos.domain.models

data class BillingTotals(
    val subtotal: Double,
    val taxes: Double,
    val discount: Double,
    val shipping: Double,
    val total: Double
)