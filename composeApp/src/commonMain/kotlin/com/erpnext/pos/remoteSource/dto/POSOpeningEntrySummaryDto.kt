package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class POSOpeningEntrySummaryDto(
    @SerialName("name")
    val name: String,
    @SerialName("pos_profile")
    val posProfile: String,
    val user: String? = null,
    val status: String? = null,
    @SerialName("docstatus")
    val docStatus: Int? = null,
    @SerialName("period_start_date")
    val periodStartDate: String? = null
)
