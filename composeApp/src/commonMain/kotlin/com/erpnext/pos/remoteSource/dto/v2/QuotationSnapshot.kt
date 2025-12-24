package com.erpnext.pos.remoteSource.dto.v2

data class QuotationSnapshot(
    val quotationId: String,
    val transactionDate: String,
    val validUntil: String?,
    val company: String,
    val partyName: String,
    val customerName: String,
    val territory: String?,
    val status: String,
    val priceListCurrency: String,
    val sellingPriceList: String?,
    val netTotal: Double,
    val grandTotal: Double
)
