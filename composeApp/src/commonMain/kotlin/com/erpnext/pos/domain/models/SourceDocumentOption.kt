package com.erpnext.pos.domain.models

import com.erpnext.pos.views.salesflow.SalesFlowSource

data class SourceDocumentItem(
    val itemCode: String,
    val itemName: String?,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val amount: Double,
    val warehouse: String?
)

data class SourceDocumentTax(
    val chargeType: String,
    val accountHead: String,
    val rate: Double,
    val taxAmount: Double
)

data class SourceDocumentTotals(
    val netTotal: Double?,
    val grandTotal: Double?,
    val taxTotal: Double?,
    val currency: String?
)

data class SourceDocumentOption(
    val id: String,
    val sourceType: SalesFlowSource,
    val customerId: String?,
    val customerName: String?,
    val date: String?,
    val status: String?,
    val items: List<SourceDocumentItem> = emptyList(),
    val taxes: List<SourceDocumentTax> = emptyList(),
    val totals: SourceDocumentTotals? = null
)
