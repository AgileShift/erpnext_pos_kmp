package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerAddressDto(
    @SerialName("name")
    val addressId: String,
    @SerialName("address_title")
    val title: String,
    @SerialName("address_type")
    val type: String,
    @SerialName("address_line1")
    val line1: String,
    @SerialName("address_line2")
    val line2: String? = null,
    @SerialName("city")
    val city: String,
    @SerialName("country")
    val country: String
)
