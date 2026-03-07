package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class POSClosingEntryResponse(@SerialName("name") val name: String)

@Serializable
data class POSClosingEntrySummaryDto(
    @SerialName("name") val name: String,
    @SerialName("pos_opening_entry") val posOpeningEntry: String? = null,
    @SerialName("period_end_date") val periodEndDate: String? = null,
    @SerialName("posting_date") val postingDate: String? = null,
    val docstatus: Int? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class POSClosingEntryDto(
    // Contracto actual de closing_create_submit: payload minimo calculado por backend.
    @SerialName("pos_opening_entry") val posOpeningEntry: String,
    @SerialName("period_end_date") val periodEndDate: String,
    @SerialName("payment_reconciliation") val paymentReconciliation: List<PaymentReconciliationDto>,
)

@Serializable
data class POSClosingSalesInvoiceDto(
    @SerialName("sales_invoice") val salesInvoice: String,
    @SerialName("posting_date") val postingDate: String? = null,
    val customer: String? = null,
    @SerialName("grand_total") val grandTotal: Double? = null,
    @SerialName("paid_amount") val paidAmount: Double? = null,
    @SerialName("outstanding_amount") val outstandingAmount: Double? = null,
    @SerialName("is_return") val isReturn: Boolean? = null,
)

@Serializable
data class PaymentReconciliationDto(
    @SerialName("mode_of_payment") val modeOfPayment: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("opening_amount")
    val openingAmount: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("expected_amount")
    val expectedAmount: Double? = null,
    @SerialName("closing_amount") val closingAmount: Double,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val difference: Double? = null,
)
