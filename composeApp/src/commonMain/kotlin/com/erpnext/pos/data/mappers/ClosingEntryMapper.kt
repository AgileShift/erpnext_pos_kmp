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
    val invoiceDetails = invoices.mapNotNull { invoice ->
        val name = invoice.invoiceName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        POSClosingInvoiceDto(
            salesInvoice = name,
            postingDate = invoice.postingDate,
            customer = invoice.customer,
            grandTotal = invoice.grandTotal,
            paidAmount = invoice.paidAmount,
            outstandingAmount = invoice.outstandingAmount,
            isReturn = invoice.isReturn
        )
    }
    return POSClosingEntryDto(
        posProfile = cashbox.posProfile,
        posOpeningEntry = openingEntryId,
        user = cashbox.user,
        company = cashbox.company,
        postingDate = postingDate,
        periodStartDate = cashbox.periodStartDate,
        periodEndDate = periodEndDate,
        balanceDetails = balanceDetails.toDto(),
        posTransactions = invoiceDetails
    )
}
