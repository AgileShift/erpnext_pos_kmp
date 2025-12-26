package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesInvoiceSnapshot(
    @SerialName("name")
    val invoiceId: String,
    @SerialName("customer")
    val customerId: String,
    @SerialName("company")
    val company: String,
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double? = null,
    @SerialName("grand_total")
    val grandTotal: Double,
    @SerialName("paid_amount")
    val paidAmount: Double? = null,
    @SerialName("net_total")
    val netTotal: Double,
    @SerialName("is_pos")
    val isPos: Boolean = true,
    @SerialName("pos_profile")
    val posProfile: String? = null,
    @SerialName("docstatus")
    val docStatus: Int? = null,
    @SerialName("route")
    val route: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("contact_display")
    val contactDisplay: String? = null,
    @SerialName("contact_mobile")
    val contactMobile: String? = null,
    @SerialName("party_account_currency")
    val currency: String? = null,
    @SerialName("modified")
    val modified: String? = null
)
