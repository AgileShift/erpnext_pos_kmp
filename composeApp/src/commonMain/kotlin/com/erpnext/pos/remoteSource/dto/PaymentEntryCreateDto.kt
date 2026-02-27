package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PaymentEntryCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("payment_type")
    val paymentType: String,
    @SerialName("party_type")
    val partyType: String,
    @SerialName("party")
    val partyId: String,
    @SerialName("mode_of_payment")
    val modeOfPayment: String? = null,
    @SerialName("paid_amount")
    val paidAmount: Double,
    @SerialName("received_amount")
    val receivedAmount: Double,
    @SerialName("paid_from")
    val paidFrom: String? = null,
    @SerialName("paid_to")
    val paidTo: String? = null,
    @SerialName("paid_to_account_currency")
    val paidToAccountCurrency: String? = null,
    @SerialName("source_exchange_rate")
    val sourceExchangeRate: Double? = null,
    @SerialName("target_exchange_rate")
    val targetExchangeRate: Double? = null,
    @SerialName("reference_no")
    val referenceNo: String? = null,
    @SerialName("reference_date")
    val referenceDate: String? = null,
    @SerialName("references")
    val references: List<PaymentEntryReferenceCreateDto> = emptyList(),
    @SerialName("docstatus")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val docStatus: Int? = null
)

@Serializable
data class PaymentEntryReferenceCreateDto(
    @SerialName("reference_doctype")
    val referenceDoctype: String,
    @SerialName("reference_name")
    val referenceName: String,
    @SerialName("total_amount")
    val totalAmount: Double? = null,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double? = null,
    @SerialName("allocated_amount")
    val allocatedAmount: Double
)
