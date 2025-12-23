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
    @SerialName("credit_limit")
    var creditLimit: Double? = null,

    var availableCredit: Double? = null,
    var pendingInvoicesCount: Int? = null,
    var totalPendingAmount: Double? = null
)

@Serializable
data class ContactChildDto(
    @SerialName("name")
    val name: String,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("email_id")
    val email: String? = null
)