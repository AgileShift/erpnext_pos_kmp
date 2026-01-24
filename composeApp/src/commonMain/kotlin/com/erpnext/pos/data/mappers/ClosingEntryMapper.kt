package com.erpnext.pos.data.mappers

import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSClosingInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toDto

fun buildClosingEntryDto(
    cashbox: CashboxEntity,
    openingEntryId: String,
    postingDate: String,
    periodEndDate: String,
    balanceDetails: List<BalanceDetailsEntity>,
    invoices: List<SalesInvoiceEntity>
): POSClosingEntryDto {
    val invoiceDetails = buildClosingInvoiceRows(invoices)
    return POSClosingEntryDto(
        posProfile = cashbox.posProfile,
        posOpeningEntry = openingEntryId,
        user = cashbox.user,
        company = cashbox.company,
        postingDate = postingDate,
        periodStartDate = cashbox.periodStartDate,
        periodEndDate = periodEndDate,
        balanceDetails = balanceDetails.toDto(),
        posTransactions = invoiceDetails,
        docStatus = 0
    )
}

private fun buildClosingInvoiceRows(
    invoices: List<SalesInvoiceEntity>
): List<POSClosingInvoiceDto> {
    val seen = mutableSetOf<String>()
    return invoices.mapNotNull { invoice ->
        val rawName = invoice.invoiceName?.trim() ?: return@mapNotNull null
        if (!isRemoteInvoiceName(rawName)) return@mapNotNull null
        if (invoice.docstatus != 1) return@mapNotNull null
        if (!invoice.isPos) return@mapNotNull null
        if (!seen.add(rawName)) return@mapNotNull null
        POSClosingInvoiceDto(
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
