package com.erpnext.pos.remoteSource.dto.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    @SerialName("name")
    val customerId: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("territory")
    val territory: String,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("customer_type")
    val customerType: String,
    @SerialName("customer_group")
    val customerGroup: String? = null,
    @SerialName("default_currency")
    val defaultCurrency: String? = null,
    @SerialName("default_price_list")
    val defaultPriceList: String? = null,
    @SerialName("primary_address")
    val primaryAddressId: String? = null,
    @SerialName("disabled")
    val disabled: Boolean = false,
    @SerialName("credit_limits")
    val creditLimits: List<CustomerCreditLimitDto> = emptyList(),
    val primaryContact: CustomerContactDto? = null,
    val primaryAddress: CustomerAddressDto? = null
) {
    fun creditLimitForCompany(companyId: String): CustomerCreditLimitDto? {
        val target = companyId.trim()
        if (target.isBlank()) return null
        return creditLimits.firstOrNull { limit ->
            limit.company?.trim()?.equals(target, ignoreCase = true) == true
        }
    }
}
