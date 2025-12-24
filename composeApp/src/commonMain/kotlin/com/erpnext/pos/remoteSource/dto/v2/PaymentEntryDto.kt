package com.erpnext.pos.remoteSource.dto.v2

data class PaymentEntryDto(
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
    val unallocatedAmount: Double? = null
)

data class PaymentEntryReferenceDto(
    val paymentEntryId: String,
    val referenceDoctype: String,
    val referenceName: String,
    val totalAmount: Double,
    val outstandingAmount: Double,
    val allocatedAmount: Double
)
