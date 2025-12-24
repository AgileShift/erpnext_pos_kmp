package com.erpnext.pos.remoteSource.dto.v2

data class DeliveryNoteSnapshot(
    val deliveryNoteId: String,
    val postingDate: String,
    val company: String,
    val customerId: String,
    val customerName: String,
    val territory: String?,
    val status: String,
    val setWarehouse: String?
)
