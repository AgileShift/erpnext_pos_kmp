package com.erpnext.pos.remoteSource.dto.v2

data class SalesOrderSnapshot(
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
