package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesOrderListDto(
    @SerialName("name") val name: String,
    @SerialName("customer") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    val status: String? = null
)

@Serializable
data class DeliveryNoteListDto(
    @SerialName("name") val name: String,
    @SerialName("customer") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("posting_date") val postingDate: String? = null,
    val status: String? = null
)

@Serializable
data class QuotationListDto(
    @SerialName("name") val name: String,
    @SerialName("party_name") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    val status: String? = null
)
