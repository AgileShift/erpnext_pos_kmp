package com.erpnext.pos.utils

import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import kotlin.math.ceil
import kotlin.math.pow

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
    val scale = when (normalized) {
        "USD" -> 2
        "NIO" -> 2
        else -> 2
    }
    return roundToCurrency(value, scale)
}

fun resolveRoundedTotal(grandTotal: Double, currency: String?): RoundedTotal {
    if (!grandTotal.isFinite()) return RoundedTotal(grandTotal, 0.0)
    val normalized = normalizeCurrency(currency).uppercase()
    val roundedTotal = when (normalized) {
        // Política de caja: total final hacia arriba a entero.
        "USD", "NIO" -> roundUpToScale(grandTotal, 0)
        else -> roundForCurrency(grandTotal, currency)
    }
    val adjustment = roundToCurrency(roundedTotal - grandTotal)
    return RoundedTotal(roundedTotal = roundedTotal, roundingAdjustment = adjustment)
}

private fun roundUpToScale(value: Double, scale: Int): Double {
    if (scale <= 0) return ceil(value)
    val factor = 10.0.pow(scale.toDouble())
    return ceil(value * factor) / factor
}
