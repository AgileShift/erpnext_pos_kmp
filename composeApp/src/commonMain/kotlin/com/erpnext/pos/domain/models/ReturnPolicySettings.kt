package com.erpnext.pos.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class ReturnDestinationPolicy {
    REFUND,
    CREDIT
}

@Serializable
data class ReturnPolicySettings(
    val maxDaysAfterInvoice: Int = 0,
    val requireReason: Boolean = false,
    val allowPartialReturns: Boolean = true,
    val allowFullReturns: Boolean = true,
    val allowRefunds: Boolean = true,
    val requirePaidInvoiceForRefund: Boolean = true,
    val defaultDestination: ReturnDestinationPolicy = ReturnDestinationPolicy.CREDIT
)

