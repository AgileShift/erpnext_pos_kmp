package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.localSource.dao.SyncStatus

data class InvoiceSnapshot(
    val invoiceId: String,
    val customerId: String,
    val postingDate: String,
    val dueDate: String,
    val status: String,
    val grandTotal: Double,
    val outstandingAmount: Double,
    val payments: Double,
    val docStatus: Int,
    val syncStatus: SyncStatus
)