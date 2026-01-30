package com.erpnext.pos.utils

/**
 * Currency helpers centralized for ERPNext rules:
 * - Totals are in invoice currency.
 * - Paid/Outstanding are in receivable (party account) currency.
 * - conversionRate is invoice -> receivable.
 */
object CurrencyService {

    fun normalize(code: String?): String? = code?.trim()?.uppercase()?.takeIf { it.isNotBlank() }

    fun resolveInvoiceToReceivableRate(
        invoiceCurrency: String?,
        receivableCurrency: String?,
        conversionRate: Double?,
        customExchangeRate: Double?
    ): Double? {
        val invoice = normalize(invoiceCurrency)
        val receivable = normalize(receivableCurrency)
        if (invoice.isNullOrBlank() || receivable.isNullOrBlank()) return null
        if (invoice.equals(receivable, ignoreCase = true)) return 1.0
        conversionRate?.takeIf { it > 0.0 }?.let { return it }
        return null
    }

    fun resolveReceivableToInvoiceRate(
        invoiceCurrency: String?,
        receivableCurrency: String?,
        conversionRate: Double?,
        customExchangeRate: Double?
    ): Double? {
        val invToRc = resolveInvoiceToReceivableRate(
            invoiceCurrency = invoiceCurrency,
            receivableCurrency = receivableCurrency,
            conversionRate = conversionRate,
            customExchangeRate = customExchangeRate
        )
        if (invToRc == null || invToRc == 0.0) return null
        return 1.0 / invToRc
    }

    fun amountInvoiceToReceivable(amount: Double, rateInvToRc: Double?): Double {
        return if (rateInvToRc != null && rateInvToRc > 0.0) amount * rateInvToRc else amount
    }

    fun amountReceivableToInvoice(amount: Double, rateInvToRc: Double?): Double {
        return if (rateInvToRc != null && rateInvToRc > 0.0) amount / rateInvToRc else amount
    }

    fun resolveDisplayCurrencies(
        supported: List<String>,
        invoiceCurrency: String?,
        receivableCurrency: String?,
        posCurrency: String?
    ): List<String> {
        return (supported + listOfNotNull(invoiceCurrency, receivableCurrency, posCurrency))
            .mapNotNull { normalize(it) }
            .distinct()
    }

    suspend fun convertFromReceivable(
        amount: Double,
        receivableCurrency: String,
        targetCurrency: String,
        invoiceCurrency: String?,
        conversionRate: Double?,
        customExchangeRate: Double?,
        rateResolver: suspend (from: String, to: String) -> Double?
    ): Double? {
        val from = normalize(receivableCurrency) ?: return null
        val to = normalize(targetCurrency) ?: return null
        if (from.equals(to, true)) return amount

        val invoice = normalize(invoiceCurrency)
        val invToRc = resolveInvoiceToReceivableRate(
            invoiceCurrency = invoice,
            receivableCurrency = from,
            conversionRate = conversionRate,
            customExchangeRate = customExchangeRate
        )
        if (invoice != null && invoice.equals(to, true) && invToRc != null && invToRc > 0.0) {
            return amount / invToRc
        }
        if (invoice != null && invoice.equals(from, true) && invToRc != null && invToRc > 0.0) {
            return amount * invToRc
        }

        val direct = rateResolver(from, to)
        if (direct != null && direct > 0.0) return amount * direct
        val reverse = rateResolver(to, from)?.takeIf { it > 0.0 }?.let { 1 / it }
        return reverse?.let { amount * it }
    }
}
