package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.preferences.CurrencySettingsPreferences
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.CurrencyPrecisionEntry
import com.erpnext.pos.utils.CurrencyPrecisionResolver
import com.erpnext.pos.utils.CurrencyPrecisionSnapshot
import com.erpnext.pos.utils.normalizeCurrency
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

class CurrencySettingsRepository(
    private val api: APIService,
    private val preferences: CurrencySettingsPreferences
) {

    suspend fun loadCached(): CurrencyPrecisionSnapshot? {
        val cached = preferences.load()
        if (cached != null) {
            CurrencyPrecisionResolver.update(cached)
        }
        return cached
    }

    suspend fun sync(): CurrencyPrecisionSnapshot? {
        val enabledCurrencies = api.getEnabledCurrencies()
            .map { normalizeCurrency(it.name) }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("USD", "NIO") }


        val defaultPrecision = 2
        val currencyMap = mutableMapOf<String, CurrencyPrecisionEntry>()

        enabledCurrencies.forEach { code ->
            val detail = api.getCurrencyDetail(code) ?: return@forEach
            val precision = resolveCurrencyPrecision(detail, defaultPrecision)
            currencyMap[code] = CurrencyPrecisionEntry(precision = precision, cashScale = precision)
        }

        val snapshot = CurrencyPrecisionSnapshot(
            currencies = currencyMap
        )
        preferences.save(snapshot)
        CurrencyPrecisionResolver.update(snapshot)
        return snapshot
    }

    private fun resolveCurrencyPrecision(doc: JsonObject, fallback: Int): Int {
        val smallestFraction = doc.doubleOrNull(
            "smallest_currency_fraction",
            "smallest_fraction",
            "smallest_currency_fraction"
        )
        if (smallestFraction != null && smallestFraction > 0.0) {
            val decimals = decimalsFromFraction(smallestFraction)
            if (decimals != null) return decimals
        }

        val fractionUnits = doc.intOrNull("fraction_units")
        if (fractionUnits != null && fractionUnits > 0) {
            val decimals = powerOfTenDecimals(fractionUnits)
            if (decimals != null) return decimals
        }

        val numberFormat = doc.stringOrNull("number_format")
        if (!numberFormat.isNullOrBlank()) {
            val decimals = decimalsFromNumberFormat(numberFormat)
            if (decimals != null) return decimals
        }

        return fallback
    }

    private fun decimalsFromFraction(value: Double): Int? {
        val candidates = (0..6).map { it to 1.0 / 10.0.pow(it.toDouble()) }
        val match = candidates.firstOrNull { abs(value - it.second) < 1e-9 }
        return match?.first
    }

    private fun powerOfTenDecimals(value: Int): Int? {
        val log = log10(value.toDouble())
        val rounded = log.roundToInt()
        return if (abs(log - rounded) < 1e-9) rounded else null
    }

    private fun decimalsFromNumberFormat(format: String): Int? {
        val dotIdx = format.lastIndexOf('.')
        if (dotIdx == -1) return 0
        val tail = format.substring(dotIdx + 1)
        val count = tail.count { it == '#' || it == '0' }
        return count.takeIf { it >= 0 }
    }
}

private fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.intOrNull(key: String): Int? {
    val prim = this[key]?.jsonPrimitive ?: return null
    return prim.intOrNull ?: prim.contentOrNull?.toIntOrNull()
}

private fun JsonObject.doubleOrNull(vararg keys: String): Double? {
    for (key in keys) {
        val prim = this[key]?.jsonPrimitive
        val value = prim?.doubleOrNull ?: prim?.contentOrNull?.toDoubleOrNull()
        if (value != null) return value
    }
    return null
}
