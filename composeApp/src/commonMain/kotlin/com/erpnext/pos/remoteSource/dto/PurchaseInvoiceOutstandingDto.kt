package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseInvoiceOutstandingDto(
    val name: String,
    val supplier: String? = null,
    @SerialName("supplier_name")
    val supplierName: String? = null,
    @SerialName("posting_date")
    val postingDate: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    val currency: String? = null,
    val status: String? = null,
    @SerialName("grand_total")
    val grandTotal: Double = 0.0,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double = 0.0
)
