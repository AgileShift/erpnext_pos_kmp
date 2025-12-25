package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesOrderSnapshot(
    @SerialName("name")
    val salesOrderId: String,
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("delivery_date")
    val deliveryDate: String? = null,
    @SerialName("company")
    val company: String,
    @SerialName("customer")
    val customerId: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("delivery_status")
    val deliveryStatus: String? = null,
    @SerialName("billing_status")
    val billingStatus: String? = null,
    @SerialName("price_list_currency")
    val priceListCurrency: String,
    @SerialName("selling_price_list")
    val sellingPriceList: String? = null,
    @SerialName("net_total")
    val netTotal: Double,
    @SerialName("grand_total")
    val grandTotal: Double
)
