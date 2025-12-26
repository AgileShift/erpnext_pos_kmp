package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentEntrySnapshot(
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
    val unallocatedAmount: Double? = null,
    @SerialName("references")
    val references: List<PaymentEntryReferenceDto> = emptyList()
)
