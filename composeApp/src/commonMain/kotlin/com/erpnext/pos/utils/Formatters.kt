package com.erpnext.pos.utils

import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.roundForCurrency
import com.erpnext.pos.utils.roundToCurrency
import kotlin.math.abs

expect fun formatAmount(symbol: String, amount: Double): String

fun formatCurrency(code: String, amount: Double): String {
    val symbol = code.toCurrencySymbol().ifBlank { code }
    val normalized = normalizeCurrency(code).uppercase()
    val display = roundForCurrency(amount, normalized)
    val rounded2 = roundToCurrency(display, 2)
    val rounded0 = roundToCurrency(display, 0)
    val isWhole = abs(rounded2 - rounded0) < 0.005
    val decimals = when (normalized) {
        "USD", "NIO" -> 2
        else -> 2
    }
    return symbol + " " + "%.${decimals}f".format(display)
}
