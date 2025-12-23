package com.erpnext.pos.remoteSource.dto.v2

data class CustomerAgingRow(
    val customerId: String,
    val outstanding: Float,
    val overdue: Float,
    val lastPurchaseDate: String?
)
