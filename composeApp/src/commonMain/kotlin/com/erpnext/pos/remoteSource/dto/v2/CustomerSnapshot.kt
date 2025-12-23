package com.erpnext.pos.remoteSource.dto.v2

data class CustomerSnapshot(
    val customerId: String,
    val customerName: String,
    val territoryId: String,
    val mobileFallback: String?,
    val credit: CustomerCreditDto,
    val primaryContact: CustomerContactDto?,
    val primaryAddress: CustomerAddressDto?,

    val creditLimit: Double,
    val outstandingAmount: Double,
    val overdueAmount: Double,

    val primaryPhone: String?,

    val lastPurchaseDate: String?
)