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
    @SerialName("balance_details")
    val balanceDetails: List<BalanceDetailsDto>,
    @SerialName("pos_transactions")
    val posTransactions: List<POSClosingInvoiceDto> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("docstatus")
    val docStatus: Int? = null
)

@Serializable
data class POSClosingInvoiceDto(
    @SerialName("sales_invoice")
    val salesInvoice: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("pos_invoice")
    val posInvoice: String? = null,
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
