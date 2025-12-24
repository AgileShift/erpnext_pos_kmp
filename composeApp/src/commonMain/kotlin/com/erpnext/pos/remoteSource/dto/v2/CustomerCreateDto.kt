package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerCreateDto(
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("customer_type")
    val customerType: String,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("customer_group")
    val customerGroup: String? = null,
    @SerialName("default_currency")
    val defaultCurrency: String? = null,
    @SerialName("default_price_list")
    val defaultPriceList: String? = null,
    @SerialName("mobile_no")
    val mobileNo: String? = null
)
