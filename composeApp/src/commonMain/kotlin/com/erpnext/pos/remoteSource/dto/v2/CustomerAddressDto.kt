package com.erpnext.pos.remoteSource.dto.v2

data class CustomerAddressDto(
    val addressId: String,
    val title: String,
    val type: String,
    val line1: String,
    val line2: String?,
    val city: String,
    val country: String
)