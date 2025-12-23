package com.erpnext.pos.domain.models

data class CashboxBO(
    val posProfile: String,
    val company: String,
    val periodStartDate: Long,
    val user: String,
    val status: Boolean,
    val balanceDetails: List<BalanceDetailsBO>
)

data class BalanceDetailsBO(
    val modeOfPayment: String,
    val openingAmount: Double
)