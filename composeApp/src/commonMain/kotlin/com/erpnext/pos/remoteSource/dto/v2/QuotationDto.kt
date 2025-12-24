package com.erpnext.pos.remoteSource.dto.v2

data class QuotationHeaderDto(
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

data class QuotationItemDto(
    val quotationId: String,
    val rowId: Int,
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val amount: Double,
    val warehouse: String?
)

data class QuotationTaxDto(
    val quotationId: String,
    val chargeType: String,
    val accountHead: String,
    val rate: Double,
    val taxAmount: Double
)

data class QuotationCustomerLinkDto(
    val quotationId: String,
    val partyName: String,
    val customerName: String,
    val contactId: String?,
    val addressId: String?
)
