package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class POSClosingEntryResponse(
    @SerialName("name")
    val name: String
)

@Serializable
data class POSClosingEntrySummaryDto(
    @SerialName("name")
    val name: String,
    @SerialName("pos_opening_entry")
    val posOpeningEntry: String? = null,
    @SerialName("period_end_date")
    val periodEndDate: String? = null,
    @SerialName("posting_date")
    val postingDate: String? = null,
    val docstatus: Int? = null
)

@Serializable
data class POSClosingEntryDto(
    @SerialName("pos_profile")
    val posProfile: String,
    @SerialName("pos_opening_entry")
    val posOpeningEntry: String,
    val user: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("company")
    val company: String,
    @SerialName("period_start_date")
    val periodStartDate: String,
    @SerialName("period_end_date")
    val periodEndDate: String,
    @SerialName("payment_reconciliation")
    val paymentReconciliation: List<PaymentReconciliationDto>,
    @SerialName("sales_invoices")
    val salesInvoices: List<POSClosingSalesInvoiceDto> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("docstatus")
    val docStatus: Int? = null
)

@Serializable
data class POSClosingSalesInvoiceDto(
    @SerialName("sales_invoice")
    val salesInvoice: String,
    @SerialName("posting_date")
    val postingDate: String? = null,
    val customer: String? = null,
    @SerialName("grand_total")
    val grandTotal: Double? = null,
    @SerialName("paid_amount")
    val paidAmount: Double? = null,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double? = null,
    @SerialName("is_return")
    val isReturn: Boolean? = null
)

@Serializable
data class PaymentReconciliationDto(
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    @SerialName("opening_amount")
    val openingAmount: Double,
    @SerialName("expected_amount")
    val expectedAmount: Double,
    @SerialName("closing_amount")
    val closingAmount: Double,
    val difference: Double
)
