package com.erpnext.pos.domain.models

import com.erpnext.pos.domain.usecases.InvoiceCancellationAction

data class InvoiceReturnLine(
    val itemCode: String,
    val quantity: Double
)

sealed interface InvoiceRefundTarget {
    object StoreCredit : InvoiceRefundTarget
    data class PaymentMode(val modeOfPayment: String) : InvoiceRefundTarget
}

data class InvoiceHistoryReturnInput(
    val invoiceId: String,
    val action: InvoiceCancellationAction,
    val reason: String? = null,
    val returnLines: List<InvoiceReturnLine> = emptyList(),
    val refundTarget: InvoiceRefundTarget = InvoiceRefundTarget.StoreCredit
)
