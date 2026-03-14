package com.erpnext.pos.printing.templates

import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine
import com.erpnext.pos.domain.printing.model.ReceiptSection
import com.erpnext.pos.domain.printing.model.ReceiptTotals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun buildPendingInvoicePaymentReceipt(
    invoiceId: String,
    amount: Double,
    currencyCode: String,
    modeOfPayment: String,
    referenceNo: String?,
    notes: String?,
): ReceiptDocument {
  val rightAmount = formatReceiptAmount(amount, currencyCode)
  val body =
      buildList {
        add(ReceiptLine("Invoice", invoiceId))
        add(ReceiptLine("Mode", modeOfPayment.ifBlank { "N/A" }))
        add(ReceiptLine("Amount", rightAmount))
        add(ReceiptLine("Date", nowDateLabel()))
        referenceNo?.trim()?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine("Reference", it)) }
        notes?.trim()?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine("Note", it.take(40))) }
      }

  return ReceiptDocument(
      documentId = "pending-payment-${Clock.System.now().toEpochMilliseconds()}",
      header =
          ReceiptSection(
              lines = listOf("ERPNext POS", "Pending Invoice Payment"),
              alignment = PrintAlignment.CENTER,
          ),
      bodyLines = body,
      totals = ReceiptTotals(total = rightAmount),
      footer =
          ReceiptSection(
              lines = listOf("Payment registered locally"),
              alignment = PrintAlignment.CENTER,
          ),
  )
}

@OptIn(ExperimentalTime::class)
fun buildBillingSaleReceipt(
    invoiceLabel: String,
    customerLabel: String,
    currencyCode: String,
    itemLines: List<Pair<String, Double>>,
    subtotal: Double,
    taxes: Double,
    total: Double,
    paidAmount: Double,
    balanceDue: Double,
): ReceiptDocument {
  val body =
      buildList {
        add(ReceiptLine("Invoice", invoiceLabel))
        add(ReceiptLine("Customer", customerLabel.take(24)))
        add(ReceiptLine("Date", nowDateLabel()))
        itemLines.forEach { (label, amount) ->
          add(ReceiptLine(label.take(24), formatReceiptAmount(amount, currencyCode)))
        }
      }

  return ReceiptDocument(
      documentId = "billing-sale-${Clock.System.now().toEpochMilliseconds()}",
      header =
          ReceiptSection(
              lines = listOf("ERPNext POS", "Sales Receipt"),
              alignment = PrintAlignment.CENTER,
          ),
      bodyLines = body,
      totals =
          ReceiptTotals(
              subTotal = formatReceiptAmount(subtotal, currencyCode),
              tax = formatReceiptAmount(taxes, currencyCode),
              total = formatReceiptAmount(total, currencyCode),
          ),
      footer =
          ReceiptSection(
              lines =
                  listOf(
                      "Paid: ${formatReceiptAmount(paidAmount, currencyCode)}",
                      "Balance: ${formatReceiptAmount(balanceDue, currencyCode)}",
                  ),
              alignment = PrintAlignment.CENTER,
          ),
  )
}

private fun formatReceiptAmount(amount: Double, currencyCode: String): String {
  val scaled = kotlin.math.round(amount * 100.0).toLong()
  val sign = if (scaled < 0) "-" else ""
  val absScaled = kotlin.math.abs(scaled)
  val whole = absScaled / 100
  val decimal = (absScaled % 100).toString().padStart(2, '0')
  return "${currencyCode.uppercase()} $sign$whole.$decimal"
}

private fun nowDateLabel(): String = Clock.System.now().toString().substringBefore("T")
