package com.erpnext.pos.remoteSource.dto.v2

data class PaymentEntrySnapshot(
    val paymentEntryId: String,
    val postingDate: String,
    val company: String,
    val territory: String?,
    val paymentType: String,
    val modeOfPayment: String,
    val partyType: String,
    val partyId: String,
    val paidAmount: Double,
    val receivedAmount: Double,
    val unallocatedAmount: Double?
)
