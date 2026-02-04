package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanySalesTargetDto(
    @SerialName("name")
    val company: String,
    @SerialName("monthly_sales_target")
    val monthlySalesTarget: Double? = null
)
