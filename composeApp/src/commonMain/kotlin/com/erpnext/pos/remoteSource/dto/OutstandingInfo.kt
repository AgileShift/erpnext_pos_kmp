package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.Serializable

@Serializable
data class OutstandingInfo(
    val totalOutstanding: Double,
    val pendingInvoices: List<SalesInvoiceDto>
)