package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesInvoiceCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("customer")
    val customerId: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("posting_time")
    val postingTime: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("is_pos")
    val isPos: Boolean = true,
    @SerialName("update_stock")
    val updateStock: Boolean = true,
    @SerialName("set_warehouse")
    val setWarehouse: String? = null,
    @SerialName("selling_price_list")
    val sellingPriceList: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("conversion_rate")
    val conversionRate: Float? = null,
    @SerialName("naming_series")
    val namingSeries: String? = null,
    @SerialName("disable_rounded_total")
    val disableRoundedTotal: Boolean? = null,
    @SerialName("rounded_total")
    val roundedTotal: Float? = null,
    @SerialName("total_taxes_and_charges")
    val totalTaxesAndCharges: Float? = null,
    @SerialName("grand_total")
    val grandTotal: Double? = null,
    @SerialName("items")
    val items: List<SalesInvoiceItemCreateDto>,
    @SerialName("payments")
    val payments: List<SalesInvoicePaymentCreateDto> = emptyList()
)

@Serializable
data class SalesInvoiceItemCreateDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("qty")
    val qty: Double,
    @SerialName("rate")
    val rate: Double? = null,
    @SerialName("uom")
    val uom: String? = null,
    @SerialName("warehouse")
    val warehouse: String? = null,
    @SerialName("price_list_rate")
    val priceListRate: Double? = null
)

@Serializable
data class SalesInvoicePaymentCreateDto(
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("type")
    val type: String? = "Receive"
)
