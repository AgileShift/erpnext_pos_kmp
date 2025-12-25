package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeliveryNoteSnapshot(
    @SerialName("name")
    val deliveryNoteId: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("company")
    val company: String,
    @SerialName("customer")
    val customerId: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("set_warehouse")
    val setWarehouse: String? = null
)
