package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerSnapshot(
    @SerialName("name")
    val customerId: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("territory")
    val territoryId: String,
    @SerialName("mobile_no")
    val mobileFallback: String? = null,
    val credit: CustomerCreditDto = CustomerCreditDto(),
    val primaryContact: CustomerContactDto? = null,
    val primaryAddress: CustomerAddressDto? = null,
    val creditLimit: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val primaryPhone: String? = null,
    val lastPurchaseDate: String? = null
)
