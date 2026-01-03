package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.remoteSource.dto.IntAsBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerCreditLimitDto(
    @SerialName("company")
    val company: String? = null,
    @SerialName("credit_limit")
    val creditLimit: Double? = null,
    @SerialName("bypass_credit_limit_check")
    @Serializable(with = IntAsBooleanSerializer::class)
    val bypassCreditLimitCheck: Boolean = false
)