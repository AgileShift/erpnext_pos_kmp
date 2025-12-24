package com.erpnext.pos.remoteSource.dto.v2

data class DeliveryNoteHeaderDto(
    val deliveryNoteId: String,
    val postingDate: String,
    val company: String,
    val customerId: String,
    val customerName: String,
    val territory: String?,
    val status: String,
    val setWarehouse: String?
)

data class DeliveryNoteItemDto(
    val deliveryNoteId: String,
    val rowId: Int,
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val amount: Double,
    val warehouse: String?
)

data class DeliveryNoteLinkDto(
    val deliveryNoteId: String,
    val salesOrderId: String?,
    val salesInvoiceId: String?
)
