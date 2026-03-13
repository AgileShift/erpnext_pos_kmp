package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentScheduleDto
import kotlin.math.abs

private const val PAYMENT_EPSILON = 0.01

internal fun sanitizeInvoiceForPaymentEntry(invoice: SalesInvoiceDto): SalesInvoiceDto {
  val total = invoice.grandTotal.coerceAtLeast(0.0)
  val paid = (invoice.paidAmount ?: 0.0).coerceAtLeast(0.0)
  val outstanding = (invoice.outstandingAmount ?: total).coerceAtLeast(0.0)
  val looksLikeFullCredit = paid <= PAYMENT_EPSILON && abs(outstanding - total) <= PAYMENT_EPSILON
  val cleanedSchedule = invoice.paymentSchedule.filter { it.isValidForSubmit() }

  if (!looksLikeFullCredit && invoice.paymentSchedule.isNotEmpty() && cleanedSchedule.isEmpty()) {
    require(cleanedSchedule.isNotEmpty()) {
      "payment_schedule inválido: cada fila requiere invoice_portion > 0 o payment_amount > 0."
    }
  }

  val resolvedTemplate = invoice.paymentTermsTemplate?.takeIf { it.isNotBlank() }
  val resolvedStatus = if (total <= PAYMENT_EPSILON) "Paid" else "Unpaid"

  return invoice.copy(
      payments = emptyList(),
      paidAmount = 0.0,
      changeAmount = null,
      outstandingAmount = total,
      status = resolvedStatus,
      paymentSchedule = if (looksLikeFullCredit) emptyList() else cleanedSchedule,
      paymentTerms = null,
      paymentTermsTemplate = resolvedTemplate,
      isPos = false,
      isCreatedUsingPos = false,
      posProfile = null,
      posOpeningEntry = null,
  )
}

private fun SalesInvoicePaymentScheduleDto.isValidForSubmit(): Boolean {
  val hasPortion = invoicePortion > 0.0
  val hasAmount = (paymentAmount ?: 0.0) > 0.0
  return dueDate.isNotBlank() && (hasPortion || hasAmount)
}
