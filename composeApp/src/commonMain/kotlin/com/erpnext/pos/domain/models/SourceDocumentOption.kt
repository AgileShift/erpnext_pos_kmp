package com.erpnext.pos.domain.models

import com.erpnext.pos.views.salesflow.SalesFlowSource

data class SourceDocumentOption(
    val id: String,
    val sourceType: SalesFlowSource,
    val customerId: String?,
    val customerName: String?,
    val date: String?,
    val status: String?
)
