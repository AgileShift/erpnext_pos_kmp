package com.erpnext.pos.remoteSource.dto

import io.ktor.util.date.GMTDate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalanceDetailsDto(
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    @SerialName("opening_amount")
    val openingAmount: Double,
    @SerialName("closing_amount")
    val closingAmount: Double? = null
)

@Serializable
data class TaxDetailDto(
    @SerialName("account_head")
    val accountHead: String,
    val rate: Double,
    val amount: Double
)

@Serializable
data class POSOpeningEntryDto(
    @SerialName("pos_profile")
    val posProfile: String,
    @SerialName("company")
    val company: String,
    val user: String? = null,
    @SerialName("period_start_date")
    val periodStartDate: String,
    @SerialName("period_end_date")
    val postingDate: String,
    @SerialName("balance_details")
    val balanceDetails: List<BalanceDetailsDto>,
    val taxes: List<TaxDetailDto>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("docstatus")
    val docStatus: Int? = null
)

@Serializable
data class POSOpeningEntryResponseDto(
    @SerialName("name")
    val name: String
)
