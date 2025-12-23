package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemPriceDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("uom")
    val uom: String,
    @SerialName("price_list")
    val priceList: String,
    @SerialName("price_list_rate")
    val priceListRate: Double,
    @SerialName("selling")
    @Serializable(with = IntAsBooleanSerializer::class)
    val selling: Boolean = true,
    @SerialName("currency")
    val currency: String
)