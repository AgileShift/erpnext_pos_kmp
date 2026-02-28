package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

internal fun sanitizeInvoiceForPaymentEntry(invoice: SalesInvoiceDto): SalesInvoiceDto {
  val total = invoice.grandTotal.coerceAtLeast(0.0)
  val resolvedStatus = if (total <= 0.01) "Paid" else "Unpaid"
  return invoice.copy(
      payments = emptyList(),
      paidAmount = 0.0,
      changeAmount = null,
      outstandingAmount = total,
      status = resolvedStatus,
      isPos = false,
      isCreatedUsingPos = false,
      posProfile = null,
      posOpeningEntry = null,
  )
}
