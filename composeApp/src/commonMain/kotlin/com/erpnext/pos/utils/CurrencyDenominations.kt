package com.erpnext.pos.utils

import com.erpnext.pos.domain.models.PaymentModesBO

data class CashDenomination(val value: Double, val label: String)
data class CurrencyDenominations(val bills: List<Double>, val coins: List<Double>)

object DenominationCatalog {
    private val catalog: Map<String, CurrencyDenominations> = mapOf(
        "USD" to CurrencyDenominations(
            bills = listOf(100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0),
            coins = emptyList()
        ),
        "NIO" to CurrencyDenominations(
            bills = listOf(1000.0, 500.0, 200.0, 100.0, 50.0, 20.0, 10.0),
            coins = listOf(5.0, 1.0, 0.5, 0.25)
        ),
        "EUR" to CurrencyDenominations(
            bills = listOf(500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0),
            coins = listOf(2.0, 1.0, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01)
        ),
    )

    fun forCurrency(code: String): CurrencyDenominations =
        catalog[normalizeCurrency(code)] ?: catalog["USD"]!!
}

fun normalizeCurrency(code: String): String = when (code.uppercase()) {
    "C$", "CORDOBA", "CORDOBAS", "NIO" -> "NIO"
    "$", "USD", "DOLAR", "DOLARES" -> "USD"
    "EUR", "EURO", "EUROS" -> "EUR"
    "GBP", "LIBRA", "LIBRAS" -> "GBP"
    else -> code.uppercase()
}


fun denominationsFor(currency: String): List<CashDenomination> {
    val def = DenominationCatalog.forCurrency(currency)
    return (def.bills + def.coins).map { value ->
        val label =
            if (value % 1.0 == 0.0) value.toInt().toString() else formatDoubleToString(value, 2)
        CashDenomination(value, label)
    }
}

fun currencyChoices(base: String, available: List<String>): List<String> {
    val normalizedBase = normalizeCurrency(base)
    val merged = (available + normalizedBase).map { normalizeCurrency(it) }.distinct()
    return merged.ifEmpty { listOf(normalizedBase) }
}

fun availableCurrencies(baseCurrency: String, paymentModes: List<PaymentModesBO>): List<String> {
    val base = normalizeCurrency(baseCurrency)
    val fromModes = paymentModes.mapNotNull { mode ->
        val text = mode.modeOfPayment.uppercase()
        when {
            text.contains("NIO") || text.contains("C$") -> "NIO"
            text.contains("USD") || text.contains("$") -> "USD"
            text.contains("EUR") -> "EUR"
            text.contains("GBP") -> "GBP"
            else -> null
        }
    }
    return (listOf(base) + fromModes).map { normalizeCurrency(it) }.distinct()
}
