package com.erpnext.pos.domain.models

data class DiscountInfo(
    val amount: Double,
    val percent: Double?,
    val source: DiscountSource
)
