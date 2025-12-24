package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesOrderCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("customer")
    val customerId: String,
    @SerialName("delivery_date")
    val deliveryDate: String? = null,
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("selling_price_list")
    val sellingPriceList: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("items")
    val items: List<SalesOrderItemCreateDto>,
    @SerialName("taxes")
    val taxes: List<SalesOrderTaxCreateDto> = emptyList()
)

@Serializable
data class SalesOrderItemCreateDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("qty")
    val qty: Double,
    @SerialName("rate")
    val rate: Double? = null,
    @SerialName("uom")
    val uom: String? = null,
    @SerialName("warehouse")
    val warehouse: String? = null
)

@Serializable
data class SalesOrderTaxCreateDto(
    @SerialName("charge_type")
    val chargeType: String,
    @SerialName("account_head")
    val accountHead: String,
    @SerialName("rate")
    val rate: Double? = null,
    @SerialName("tax_amount")
    val taxAmount: Double? = null
)
