package com.erpnext.pos.utils

import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.moneyScaleBankers
import com.erpnext.pos.utils.oauth.toDouble
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyPrecisionEntry(
    val precision: Int,
    val cashScale: Int
)

@Serializable
data class CurrencyPrecisionSnapshot(
    val currencyPrecision: Int? = null,
    val floatPrecision: Int? = null,
    val roundingMethod: String? = null,
    val currencies: Map<String, CurrencyPrecisionEntry> = emptyMap()
)

object CurrencyPrecisionResolver {
    private var snapshot: CurrencyPrecisionSnapshot = CurrencyPrecisionSnapshot()

    fun update(newSnapshot: CurrencyPrecisionSnapshot) {
        snapshot = newSnapshot
    }

    fun defaultPrecision(): Int {
        return snapshot.currencyPrecision
            ?: snapshot.floatPrecision
            ?: 2
    }

    fun precisionFor(currency: String?): Int {
        val code = normalizeCurrency(currency)
        return snapshot.currencies[code]?.precision ?: defaultPrecision()
    }

    fun cashScaleFor(currency: String?): Int {
        val code = normalizeCurrency(currency)
        return snapshot.currencies[code]?.cashScale ?: precisionFor(code)
    }

    fun round(value: Double, currency: String?): Double {
        if (!value.isFinite()) return value
        val scale = precisionFor(currency)
        return roundWithScale(value, scale)
    }

    fun roundWithScale(value: Double, scale: Int): Double {
        if (!value.isFinite()) return value
        val rounded = if (isBankersRounding(snapshot.roundingMethod)) {
            bd(value).moneyScaleBankers(scale)
        } else {
            bd(value).moneyScale(scale)
        }
        return rounded.toDouble(scale)
    }

    private fun isBankersRounding(method: String?): Boolean {
        if (method.isNullOrBlank()) return false
        return method.contains("bank", ignoreCase = true) ||
            method.contains("half even", ignoreCase = true)
    }
}
