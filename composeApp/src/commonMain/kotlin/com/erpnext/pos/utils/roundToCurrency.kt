package com.erpnext.pos.utils

import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import kotlin.math.ceil
import kotlin.math.floor

fun roundToCurrency(value: Double, scale: Int = 2): Double {
    if (!value.isFinite()) return value
    return bd(value).moneyScale(scale).toDouble(scale)
}

data class RoundedTotal(
    val roundedTotal: Double,
    val roundingAdjustment: Double
)

fun roundForCurrency(value: Double, currency: String?): Double {
    if (!value.isFinite()) return value
    val normalized = normalizeCurrency(currency).uppercase()
    return when (normalized) {
        "NIO" -> ceil(value)
        "USD" -> {
            val rounded2 = roundToCurrency(value, 2)
            val frac = rounded2 - floor(rounded2)
            if (frac >= 0.99 - 0.000001) ceil(rounded2) else rounded2
        }
        else -> roundToCurrency(value, 2)
    }
}

fun resolveRoundedTotal(grandTotal: Double, currency: String?): RoundedTotal {
    if (!grandTotal.isFinite()) return RoundedTotal(grandTotal, 0.0)
    val roundedTotal = roundForCurrency(grandTotal, currency)
    val adjustment = roundToCurrency(roundedTotal - grandTotal)
    return RoundedTotal(roundedTotal = roundedTotal, roundingAdjustment = adjustment)
}
