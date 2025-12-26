package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.Serializable

@Serializable
data class CustomerCreditDto(
    val creditLimit: Double? = null,
    val outstanding: Double = 0.0,
    val overdue: Double = 0.0,
    val availableCredit: Double? = null // null if creditLimit is null
)
