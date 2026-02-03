package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentEntryDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("posting_date")
    val postingDate: String? = null,
    @SerialName("party")
    val party: String? = null,
    @SerialName("party_type")
    val partyType: String? = null,
    @SerialName("payment_type")
    val paymentType: String? = null,
    @SerialName("mode_of_payment")
    val modeOfPayment: String? = null,
    @SerialName("paid_amount")
    val paidAmount: Double = 0.0,
    @SerialName("received_amount")
    val receivedAmount: Double = 0.0,
    @SerialName("paid_from_account_currency")
    val paidFromAccountCurrency: String? = null,
    @SerialName("paid_to_account_currency")
    val paidToAccountCurrency: String? = null,
    @SerialName("references")
    val references: List<PaymentEntryReferenceDto> = emptyList()
)

@Serializable
data class PaymentEntryReferenceDto(
    @SerialName("reference_doctype")
    val referenceDoctype: String? = null,
    @SerialName("reference_name")
    val referenceName: String? = null,
    @SerialName("allocated_amount")
    val allocatedAmount: Double = 0.0
)
