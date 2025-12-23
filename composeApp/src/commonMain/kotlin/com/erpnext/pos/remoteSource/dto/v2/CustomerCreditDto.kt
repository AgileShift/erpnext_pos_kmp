package com.erpnext.pos.remoteSource.dto.v2

data class CustomerCreditDto(
    val creditLimit: Double?,
    val outstanding: Double,
    val overdue: Double,
    val availableCredit: Double? // null if creditLimit is null
)