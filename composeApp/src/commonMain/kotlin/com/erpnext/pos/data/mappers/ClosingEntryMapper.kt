package com.erpnext.pos.data.mappers

import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSClosingSalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.PaymentReconciliationDto
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.PaymentReconciliationSeed

fun buildClosingEntryDto(
    cashbox: CashboxEntity,
    openingEntryId: String,
    postingDate: String,
    periodEndDate: String,
    paymentReconciliation: List<PaymentReconciliationSeed>,
    invoices: List<SalesInvoiceEntity>,
    paidInvoiceNames: Set<String> = emptySet()
): POSClosingEntryDto {
    val invoiceDetails = buildClosingSalesInvoiceRows(
        invoices = invoices,
        expectedProfile = cashbox.posProfile,
        expectedOpening = openingEntryId,
        paidInvoiceNames = paidInvoiceNames
    )
    return POSClosingEntryDto(
        posProfile = cashbox.posProfile,
        posOpeningEntry = openingEntryId,
        user = cashbox.user,
        company = cashbox.company,
        postingDate = postingDate,
        periodStartDate = cashbox.periodStartDate,
        periodEndDate = periodEndDate,
        paymentReconciliation = paymentReconciliation.map { it.toDto() },
        salesInvoices = invoiceDetails,
        docStatus = 0
    )
}

private fun buildClosingSalesInvoiceRows(
    invoices: List<SalesInvoiceEntity>,
    expectedProfile: String,
    expectedOpening: String,
    paidInvoiceNames: Set<String>
): List<POSClosingSalesInvoiceDto> {
    val seen = mutableSetOf<String>()
    return invoices.mapNotNull { invoice ->
        val rawName = invoice.invoiceName?.trim() ?: return@mapNotNull null
        if (!isRemoteInvoiceName(rawName)) return@mapNotNull null
        if (invoice.docstatus != 1) return@mapNotNull null
        if (!invoice.isPos) return@mapNotNull null
        if (!invoice.profileId.equals(expectedProfile, ignoreCase = true)) return@mapNotNull null
        if (!invoice.posOpeningEntry.equals(expectedOpening, ignoreCase = true)) return@mapNotNull null
        if (paidInvoiceNames.isNotEmpty() && !paidInvoiceNames.contains(rawName)) return@mapNotNull null
        if ((invoice.paidAmount ?: 0.0) <= 0.0001) return@mapNotNull null
        if (!seen.add(rawName)) return@mapNotNull null
        POSClosingSalesInvoiceDto(
            salesInvoice = rawName,
            postingDate = invoice.postingDate,
            customer = invoice.customer,
            grandTotal = invoice.grandTotal,
            paidAmount = invoice.paidAmount,
            outstandingAmount = invoice.outstandingAmount,
            isReturn = invoice.isReturn
        )
    }
}

private fun isRemoteInvoiceName(name: String): Boolean {
    if (name.isBlank()) return false
    if (name.startsWith("LOCAL-", ignoreCase = true)) return false
    if (name.equals("none", ignoreCase = true)) return false
    return true
}

private fun PaymentReconciliationSeed.toDto(): PaymentReconciliationDto {
    val expectedRounded = roundToCurrency(expectedAmount)
    val closingRounded = roundToCurrency(closingAmount)
    val openingRounded = roundToCurrency(openingAmount)
    val difference = roundToCurrency(closingRounded - expectedRounded)
    return PaymentReconciliationDto(
        modeOfPayment = modeOfPayment,
        openingAmount = openingRounded,
        expectedAmount = expectedRounded,
        closingAmount = closingRounded,
        difference = difference
    )
}
