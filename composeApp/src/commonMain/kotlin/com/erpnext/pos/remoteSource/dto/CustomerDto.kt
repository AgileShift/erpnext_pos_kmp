package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    @SerialName("name")
    val name: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("customer_type")
    val customerType: String,
    @SerialName("disabled")
    @Serializable(IntAsBooleanSerializer::class)
    val disabled: Boolean = false,
    @SerialName("credit_limits")
    val creditLimits: List<CustomerCreditLimitDto> = emptyList(),
    @SerialName("primary_address")
    val address: String? = null,
    @SerialName("email_id")
    val email: String? = null,
    @SerialName("image")
    val image: String? = null,

    var availableCredit: Double? = null,
    var pendingInvoicesCount: Int? = null,
    var totalPendingAmount: Double? = null
) {
    fun creditLimitForCompany(company: String?): CustomerCreditLimitDto {
        /*val target = company?.trim().orEmpty()
        if (target.isBlank()) return null
        return creditLimits.firstOrNull { limit ->
            limit.company?.trim()?.equals(target, ignoreCase = true) == true
        }*/

        return creditLimits.first()
    }
}

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

@Serializable
data class ContactChildDto(
    @SerialName("name")
    val name: String,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("email_id")
    val email: String? = null
)
