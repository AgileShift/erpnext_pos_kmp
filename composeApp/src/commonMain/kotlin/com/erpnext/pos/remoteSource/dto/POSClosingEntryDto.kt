package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class POSClosingEntryResponse(
    @SerialName("name")
    val name: String
)

@Serializable
data class POSClosingEntryDto(
    @SerialName("pos_profile")
    val posProfile: String,
    @SerialName("pos_opening_entry")
    val posOpeningEntry: String,
    val user: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("company")
    val company: String,
    @SerialName("period_start_date")
    val periodStartDate: String,
    @SerialName("period_end_date")
    val periodEndDate: String,
    @SerialName("balance_details")
    val balanceDetails: List<BalanceDetailsDto>,
    @SerialName("docstatus")
    val docStatus: Int? = 1
)