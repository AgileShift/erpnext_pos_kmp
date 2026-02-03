package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockSettingsDto(
    @SerialName("allow_negative_stock")
    @Serializable(with = IntAsBooleanSerializer::class)
    val allowNegativeStock: Boolean = false
)
