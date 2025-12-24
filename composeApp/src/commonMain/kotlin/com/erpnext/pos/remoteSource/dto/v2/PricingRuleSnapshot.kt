package com.erpnext.pos.remoteSource.dto.v2

data class PricingRuleSnapshot(
    val pricingRuleId: String,
    val priority: Int,
    val condition: String?,
    val territory: String?,
    val forPriceList: String,
    val otherItemCode: String?,
    val otherItemGroup: String?,
    val validFrom: String?,
    val validUntil: String?
)
