package com.erpnext.pos.data.mappers

import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.PaymentReconciliationDto
import com.erpnext.pos.utils.PaymentReconciliationSeed
import com.erpnext.pos.utils.roundToCurrency

fun buildClosingEntryDto(
    cashbox: CashboxEntity,
    openingEntryId: String,
    postingDate: String,
    periodEndDate: String,
    paymentReconciliation: List<PaymentReconciliationSeed>,
    @Suppress("UNUSED_PARAMETER")
    invoices: List<SalesInvoiceEntity>,
    @Suppress("UNUSED_PARAMETER")
    paidInvoiceNames: Set<String> = emptySet(),
): POSClosingEntryDto {
  // sales_invoices se calcula en API desde pos_opening_entry para evitar divergencias cliente/ERP.
  return POSClosingEntryDto(
      posOpeningEntry = openingEntryId,
      periodEndDate = periodEndDate,
      paymentReconciliation = paymentReconciliation.map { it.toDto() },
  )
}

private fun PaymentReconciliationSeed.toDto(): PaymentReconciliationDto {
  val closingRounded = roundToCurrency(closingAmount)
  return PaymentReconciliationDto(
      modeOfPayment = modeOfPayment,
      closingAmount = closingRounded,
  )
}
