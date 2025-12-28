package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeliveryChargeDto(
    val label: String,
    @SerialName("default_rate")
    val defaultRate: Double
)
