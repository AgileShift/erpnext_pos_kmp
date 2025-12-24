package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeliveryNoteCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("customer")
    val customerId: String,
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("set_warehouse")
    val setWarehouse: String? = null,
    @SerialName("items")
    val items: List<DeliveryNoteItemCreateDto>
)

@Serializable
data class DeliveryNoteItemCreateDto(
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
    @SerialName("against_sales_order")
    val salesOrderId: String? = null,
    @SerialName("sales_invoice")
    val salesInvoiceId: String? = null
)
