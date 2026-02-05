package com.erpnext.pos.utils

import kotlin.math.ceil
import kotlin.math.pow

fun roundToCurrency(value: Double, scale: Int = CurrencyPrecisionResolver.defaultPrecision()): Double {
    if (!value.isFinite()) return value
    return CurrencyPrecisionResolver.roundWithScale(value, scale)
}

data class RoundedTotal(
    val roundedTotal: Double,
    val roundingAdjustment: Double
)

fun roundForCurrency(value: Double, currency: String?): Double {
    if (!value.isFinite()) return value
    return CurrencyPrecisionResolver.round(value, currency)
}

fun resolveRoundedTotal(grandTotal: Double, currency: String?): RoundedTotal {
    if (!grandTotal.isFinite()) return RoundedTotal(grandTotal, 0.0)
    val roundedTotal = roundForCurrency(grandTotal, currency)
    val adjustment = roundToCurrency(roundedTotal - grandTotal)
    return RoundedTotal(roundedTotal = roundedTotal, roundingAdjustment = adjustment)
}

private fun roundUpToScale(value: Double, scale: Int): Double {
    if (scale <= 0) return ceil(value)
    val factor = 10.0.pow(scale.toDouble())
    return ceil(value * factor) / factor
}
