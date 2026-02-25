package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShippingRuleDto(
    val label: String,
    @SerialName("shipping_amount")
    val defaultRate: Double
)
