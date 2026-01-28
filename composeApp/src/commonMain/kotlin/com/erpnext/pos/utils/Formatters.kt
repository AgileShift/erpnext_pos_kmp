package com.erpnext.pos.utils

expect fun formatAmount(symbol: String, amount: Double): String

fun formatCurrency(code: String, amount: Double): String {
    val symbol = code.toCurrencySymbol().ifBlank { code }
    return formatAmount(symbol, amount)
}
