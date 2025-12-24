package com.erpnext.pos.remoteSource.dto.v2

data class SalesOrderHeaderDto(
    val salesOrderId: String,
    val transactionDate: String,
    val deliveryDate: String?,
    val company: String,
    val customerId: String,
    val customerName: String,
    val territory: String?,
    val status: String,
    val deliveryStatus: String?,
    val billingStatus: String?,
    val priceListCurrency: String,
    val sellingPriceList: String?,
    val netTotal: Double,
    val grandTotal: Double
)

data class SalesOrderItemDto(
    val salesOrderId: String,
    val rowId: Int,
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val amount: Double,
    val warehouse: String?
)
