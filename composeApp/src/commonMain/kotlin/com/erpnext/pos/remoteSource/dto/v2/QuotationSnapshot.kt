package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuotationSnapshot(
    @SerialName("name")
    val quotationId: String,
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("valid_till")
    val validUntil: String? = null,
    @SerialName("company")
    val company: String,
    @SerialName("party_name")
    val partyName: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("price_list_currency")
    val priceListCurrency: String,
    @SerialName("selling_price_list")
    val sellingPriceList: String? = null,
    @SerialName("net_total")
    val netTotal: Double,
    @SerialName("grand_total")
    val grandTotal: Double
)
