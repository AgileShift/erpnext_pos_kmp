package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesOrderListDto(
    @SerialName("name") val name: String,
    @SerialName("customer") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    val status: String? = null,
    @SerialName("net_total") val netTotal: Double? = null,
    @SerialName("grand_total") val grandTotal: Double? = null,
    @SerialName("total_taxes_and_charges") val totalTaxesAndCharges: Double? = null,
    @SerialName("price_list_currency") val priceListCurrency: String? = null,
    @SerialName("currency") val currency: String? = null
)

@Serializable
data class DeliveryNoteListDto(
    @SerialName("name") val name: String,
    @SerialName("customer") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("posting_date") val postingDate: String? = null,
    val status: String? = null,
    @SerialName("net_total") val netTotal: Double? = null,
    @SerialName("grand_total") val grandTotal: Double? = null,
    @SerialName("total_taxes_and_charges") val totalTaxesAndCharges: Double? = null,
    @SerialName("currency") val currency: String? = null
)

@Serializable
data class QuotationListDto(
    @SerialName("name") val name: String,
    @SerialName("party_name") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    val status: String? = null,
    @SerialName("net_total") val netTotal: Double? = null,
    @SerialName("grand_total") val grandTotal: Double? = null,
    @SerialName("total_taxes_and_charges") val totalTaxesAndCharges: Double? = null,
    @SerialName("price_list_currency") val priceListCurrency: String? = null,
    @SerialName("currency") val currency: String? = null
)
