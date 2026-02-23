package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InternalTransferCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    @SerialName("paid_amount")
    val paidAmount: Double,
    @SerialName("received_amount")
    val receivedAmount: Double,
    @SerialName("paid_from")
    val paidFrom: String,
    @SerialName("paid_to")
    val paidTo: String,
    @SerialName("reference_no")
    val referenceNo: String? = null,
    @SerialName("reference_date")
    val referenceDate: String? = null
)

@Serializable
data class InternalTransferSubmitDto(
    @SerialName("name")
    val name: String,
    @SerialName("docstatus")
    val docStatus: Int? = null,
    @SerialName("payment_type")
    val paymentType: String? = null,
    @SerialName("party_type")
    val partyType: String? = null,
    @SerialName("party")
    val party: String? = null,
    @SerialName("paid_from")
    val paidFrom: String? = null,
    @SerialName("paid_to")
    val paidTo: String? = null,
    @SerialName("paid_amount")
    val paidAmount: Double? = null,
    @SerialName("received_amount")
    val receivedAmount: Double? = null,
    @SerialName("posting_date")
    val postingDate: String? = null,
    @SerialName("modified")
    val modified: String? = null
)
