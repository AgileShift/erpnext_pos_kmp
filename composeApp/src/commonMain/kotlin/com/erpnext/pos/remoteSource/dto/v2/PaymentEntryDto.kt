package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentEntryDto(
    @SerialName("name")
    val paymentEntryId: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("company")
    val company: String,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("payment_type")
    val paymentType: String,
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    @SerialName("party_type")
    val partyType: String,
    @SerialName("party")
    val partyId: String,
    @SerialName("paid_amount")
    val paidAmount: Double,
    @SerialName("received_amount")
    val receivedAmount: Double,
    @SerialName("unallocated_amount")
    val unallocatedAmount: Double? = null
)

@Serializable
data class PaymentEntryReferenceDto(
    @SerialName("payment_entry")
    val paymentEntryId: String,
    @SerialName("reference_doctype")
    val referenceDoctype: String,
    @SerialName("reference_name")
    val referenceName: String,
    @SerialName("total_amount")
    val totalAmount: Double,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double,
    @SerialName("allocated_amount")
    val allocatedAmount: Double
)
