package com.erpnext.pos.utils

import com.erpnext.pos.localSource.dao.ShiftPaymentRow
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.views.reconciliation.UNASSIGNED_PAYMENT_MODE
import com.erpnext.pos.utils.CurrencyService

data class PaymentReconciliationSeed(
    val modeOfPayment: String,
    val openingAmount: Double,
    val expectedAmount: Double,
    val closingAmount: Double
)

suspend fun buildPaymentReconciliationSeeds(
    balanceDetails: List<BalanceDetailsEntity>,
    paymentRows: List<ShiftPaymentRow>,
    invoices: List<SalesInvoiceEntity>,
    modeCurrency: Map<String, String>,
    posCurrency: String,
    rateResolver: suspend (fromCurrency: String, toCurrency: String) -> Double?
): List<PaymentReconciliationSeed> {
    val openingByMode = balanceDetails.associate { it.modeOfPayment to roundToCurrency(it.openingAmount) }
    val closingByMode = balanceDetails.associate { detail ->
        detail.modeOfPayment to roundToCurrency(detail.closingAmount ?: detail.openingAmount)
    }
    val orderedModes = LinkedHashSet<String>()
    balanceDetails.forEach { orderedModes.add(it.modeOfPayment) }
    paymentRows.forEach { orderedModes.add(it.modeOfPayment) }
    modeCurrency.keys.forEach { orderedModes.add(it) }

    val paymentsByMode = aggregatePaymentsByMode(
        invoices = invoices,
        rows = paymentRows,
        posCurrency = posCurrency,
        modeCurrency = modeCurrency,
        rateResolver = rateResolver
    )
    val expectedByMode = buildExpectedByMode(
        openingByMode = openingByMode,
        paymentsByMode = paymentsByMode,
        availableModes = orderedModes.toList()
    )

    return orderedModes.map { mode ->
        PaymentReconciliationSeed(
            modeOfPayment = mode,
            openingAmount = openingByMode[mode] ?: 0.0,
            expectedAmount = expectedByMode[mode] ?: 0.0,
            closingAmount = closingByMode[mode] ?: 0.0
        )
    }
}

private suspend fun aggregatePaymentsByMode(
    invoices: List<SalesInvoiceEntity>,
    rows: List<ShiftPaymentRow>,
    posCurrency: String,
    modeCurrency: Map<String, String>,
    rateResolver: suspend (fromCurrency: String, toCurrency: String) -> Double?
): Map<String, Double> {
    val totals = mutableMapOf<String, Double>()
    val paymentsByInvoice = mutableMapOf<String, Double>()
    val invoiceByName = invoices.associateBy { it.invoiceName }
    rows.forEach { row ->
        val payCurrency = resolvePaymentCurrency(row, posCurrency)
        val amount = resolvePaymentAmount(row, payCurrency)
        totals[row.modeOfPayment] = (totals[row.modeOfPayment] ?: 0.0) + amount

        val invoice = invoiceByName[row.invoiceName]
        val receivableCurrency = normalizeCurrency(invoice?.partyAccountCurrency)
        val invoiceCurrency = normalizeCurrency(invoice?.currency)
        val rateInvToRc =
            CurrencyService.resolveInvoiceToReceivableRateUnified(
                invoiceCurrency = invoiceCurrency,
                receivableCurrency = receivableCurrency,
                conversionRate = invoice?.conversionRate,
                customExchangeRate = invoice?.customExchangeRate,
                posCurrency = posCurrency,
                posExchangeRate = null,
                rateResolver = { from, to -> rateResolver(from, to) }
            )
        val rowReceivable = CurrencyService.amountInvoiceToReceivable(
            row.amount,
            rateInvToRc
        )
        paymentsByInvoice[row.invoiceName] =
            (paymentsByInvoice[row.invoiceName] ?: 0.0) + rowReceivable
    }

    invoices.forEach { invoice ->
        val invoiceName = invoice.invoiceName ?: return@forEach
        val paidAmount = invoice.paidAmount
        if (paidAmount <= 0.0) return@forEach
        val invoiceCurrency = normalizeCurrency(invoice.partyAccountCurrency)
        val captured = paymentsByInvoice[invoiceName] ?: 0.0
        val delta = paidAmount - captured
        if (delta > 0.005) {
            val mode = invoice.modeOfPayment?.takeIf { it.isNotBlank() } ?: UNASSIGNED_PAYMENT_MODE
            val targetCurrency = resolveModeCurrency(mode, modeCurrency, posCurrency)
            val adjusted = if (targetCurrency.equals(invoiceCurrency, ignoreCase = true)) {
                delta
            } else {
                val rate = rateResolver(invoiceCurrency, targetCurrency) ?: 1.0
                delta * rate
            }
            totals[mode] = (totals[mode] ?: 0.0) + adjusted
        }
    }
    return totals.mapValues { roundToCurrency(it.value) }
}

private fun buildExpectedByMode(
    openingByMode: Map<String, Double>,
    paymentsByMode: Map<String, Double>,
    availableModes: List<String>
): Map<String, Double> {
    val expected = openingByMode.toMutableMap()
    availableModes.forEach { mode ->
        if (!expected.containsKey(mode)) {
            expected[mode] = 0.0
        }
    }
    paymentsByMode.forEach { (mode, amount) ->
        expected[mode] = roundToCurrency((expected[mode] ?: 0.0) + amount)
    }
    return expected.mapValues { roundToCurrency(it.value) }
}

private fun resolvePaymentCurrency(
    row: ShiftPaymentRow,
    posCurrency: String
): String {
    val fromRow = normalizeCurrency(row.paymentCurrency)
    return fromRow.takeIf { it.isNotBlank() } ?: normalizeCurrency(posCurrency)
}

private fun resolvePaymentAmount(row: ShiftPaymentRow, paymentCurrency: String): Double {
    if (row.enteredAmount > 0.0) return row.enteredAmount
    if (row.exchangeRate > 0.0 && paymentCurrency.isNotBlank()) {
        return row.amount / row.exchangeRate
    }
    return row.amount
}

private fun resolveModeCurrency(
    mode: String,
    modeCurrency: Map<String, String>,
    posCurrency: String
): String {
    val resolved = modeCurrency[mode]
    return normalizeCurrency(resolved ?: posCurrency)
}
