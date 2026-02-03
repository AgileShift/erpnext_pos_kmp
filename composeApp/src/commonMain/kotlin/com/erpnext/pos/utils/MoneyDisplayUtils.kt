package com.erpnext.pos.utils

import com.erpnext.pos.domain.models.SalesInvoiceBO

data class InvoiceDisplayAmounts(
    val invoiceCurrency: String,
    val companyCurrency: String,
    val totalInvoice: Double,
    val totalCompany: Double,
    val outstandingInvoice: Double,
    val outstandingCompany: Double
)

fun resolveInvoiceDisplayAmounts(
    invoice: SalesInvoiceBO,
    companyCurrency: String
): InvoiceDisplayAmounts {
    val invoiceCurrency = normalizeCurrency(invoice.currency) ?: normalizeCurrency(companyCurrency) ?: "USD"
    val company = normalizeCurrency(companyCurrency) ?: invoiceCurrency
    val receivable = normalizeCurrency(invoice.partyAccountCurrency) ?: company
    val totalInvoice = invoice.total
    val outstandingInvoice = invoice.outstandingAmount
    val totalCompany = invoice.baseGrandTotal ?: computeBaseAmount(
        amount = totalInvoice,
        invoiceCurrency = invoiceCurrency,
        companyCurrency = company,
        conversionRate = invoice.conversionRate
    )
    val rateInvToRc = invoice.conversionRate?.takeIf { it > 0.0 }
    val outstandingInvoiceResolved = when {
        receivable.equals(invoiceCurrency, ignoreCase = true) -> outstandingInvoice
        rateInvToRc != null -> outstandingInvoice / rateInvToRc
        else -> outstandingInvoice
    }
    val outstandingCompany = when {
        invoice.baseOutstandingAmount != null -> invoice.baseOutstandingAmount
        receivable.equals(company, ignoreCase = true) -> outstandingInvoice
        company.equals(invoiceCurrency, ignoreCase = true) -> outstandingInvoiceResolved
        else -> outstandingInvoice
    }
    return InvoiceDisplayAmounts(
        invoiceCurrency = invoiceCurrency,
        companyCurrency = company,
        totalInvoice = totalInvoice,
        totalCompany = totalCompany,
        outstandingInvoice = outstandingInvoiceResolved,
        outstandingCompany = outstandingCompany
    )
}

fun computeBaseAmount(
    amount: Double,
    invoiceCurrency: String,
    companyCurrency: String,
    conversionRate: Double?
): Double {
    if (invoiceCurrency.equals(companyCurrency, ignoreCase = true)) return amount
    val rate = conversionRate?.takeIf { it > 0.0 }
    return rate?.let { amount * it } ?: amount
}

suspend fun resolveCompanyToTargetAmount(
    amountCompany: Double,
    companyCurrency: String,
    targetCurrency: String,
    rateResolver: suspend (from: String, to: String) -> Double?
): Double? {
    val from = normalizeCurrency(companyCurrency) ?: return null
    val to = normalizeCurrency(targetCurrency) ?: return null
    if (from.equals(to, ignoreCase = true)) return amountCompany
    val rate = rateResolver(from, to)?.takeIf { it > 0.0 } ?: return null
    return amountCompany * rate
}
