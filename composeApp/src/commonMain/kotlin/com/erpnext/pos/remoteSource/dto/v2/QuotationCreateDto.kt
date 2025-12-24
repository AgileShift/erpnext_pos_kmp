package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuotationCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("quotation_to")
    val quotationTo: String = "Customer",
    @SerialName("party_name")
    val partyName: String,
    @SerialName("valid_till")
    val validTill: String? = null,
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("selling_price_list")
    val sellingPriceList: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("items")
    val items: List<QuotationItemCreateDto>,
    @SerialName("taxes")
    val taxes: List<QuotationTaxCreateDto> = emptyList()
)

@Serializable
data class QuotationItemCreateDto(
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
data class QuotationTaxCreateDto(
    @SerialName("charge_type")
    val chargeType: String,
    @SerialName("account_head")
    val accountHead: String,
    @SerialName("rate")
    val rate: Double? = null,
    @SerialName("tax_amount")
    val taxAmount: Double? = null
)
