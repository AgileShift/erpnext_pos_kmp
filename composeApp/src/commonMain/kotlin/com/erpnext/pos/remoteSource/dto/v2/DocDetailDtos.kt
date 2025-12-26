package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentScheduleRowDto(
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("payment_amount")
    val paymentAmount: Double? = null,
    @SerialName("outstanding")
    val outstandingAmount: Double? = null
)

@Serializable
data class QuotationDetailDto(
    @SerialName("name")
    val quotationId: String,
    @SerialName("items")
    val items: List<QuotationDetailItemDto> = emptyList(),
    @SerialName("taxes")
    val taxes: List<QuotationDetailTaxDto> = emptyList(),
    @SerialName("payment_schedule")
    val paymentSchedule: List<PaymentScheduleRowDto> = emptyList()
)

@Serializable
data class QuotationDetailItemDto(
    @SerialName("idx")
    val rowId: Int,
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("item_name")
    val itemName: String? = null,
    @SerialName("qty")
    val qty: Double,
    @SerialName("uom")
    val uom: String,
    @SerialName("rate")
    val rate: Double,
    @SerialName("amount")
    val amount: Double,
    @SerialName("warehouse")
    val warehouse: String? = null
)

@Serializable
data class QuotationDetailTaxDto(
    @SerialName("charge_type")
    val chargeType: String,
    @SerialName("account_head")
    val accountHead: String,
    @SerialName("rate")
    val rate: Double,
    @SerialName("tax_amount")
    val taxAmount: Double
)

@Serializable
data class SalesOrderDetailDto(
    @SerialName("name")
    val salesOrderId: String,
    @SerialName("items")
    val items: List<SalesOrderDetailItemDto> = emptyList(),
    @SerialName("payment_schedule")
    val paymentSchedule: List<PaymentScheduleRowDto> = emptyList()
)

@Serializable
data class SalesOrderDetailItemDto(
    @SerialName("idx")
    val rowId: Int,
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("item_name")
    val itemName: String? = null,
    @SerialName("qty")
    val qty: Double,
    @SerialName("uom")
    val uom: String,
    @SerialName("rate")
    val rate: Double,
    @SerialName("amount")
    val amount: Double,
    @SerialName("warehouse")
    val warehouse: String? = null
)

@Serializable
data class DeliveryNoteDetailDto(
    @SerialName("name")
    val deliveryNoteId: String,
    @SerialName("items")
    val items: List<DeliveryNoteDetailItemDto> = emptyList()
)

@Serializable
data class DeliveryNoteDetailItemDto(
    @SerialName("idx")
    val rowId: Int,
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("item_name")
    val itemName: String? = null,
    @SerialName("qty")
    val qty: Double,
    @SerialName("uom")
    val uom: String,
    @SerialName("rate")
    val rate: Double,
    @SerialName("amount")
    val amount: Double,
    @SerialName("warehouse")
    val warehouse: String? = null,
    @SerialName("sales_order")
    val salesOrderId: String? = null,
    @SerialName("sales_invoice")
    val salesInvoiceId: String? = null
)
