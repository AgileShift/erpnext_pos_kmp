package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerCreateDto(
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("customer_type")
    val customerType: String,
    @SerialName("customer_group")
    val customerGroup: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("default_currency")
    val defaultCurrency: String? = null,
    @SerialName("default_price_list")
    val defaultPriceList: String? = null,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("email_id")
    val emailId: String? = null,
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("tax_category")
    val taxCategory: String? = null,
    @SerialName("is_internal_customer")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isInternalCustomer: Boolean = false,
    @SerialName("represents_company")
    val representsCompany: String? = null,
    @SerialName("credit_limits")
    val creditLimits: List<CustomerCreditLimitCreateDto>? = null,
    @SerialName("payment_terms")
    val paymentTerms: String? = null,
    @SerialName("customer_details")
    val customerDetails: String? = null
)

@Serializable
data class CustomerCreditLimitCreateDto(
    @SerialName("company")
    val company: String,
    @SerialName("credit_limit")
    val creditLimit: Double
)

@Serializable
data class DocNameResponseDto(
    @SerialName("name")
    val name: String
)

@Serializable
data class AddressCreateDto(
    @SerialName("address_title")
    val addressTitle: String,
    @SerialName("address_type")
    val addressType: String? = "Billing",
    @SerialName("address_line1")
    val addressLine1: String? = null,
    @SerialName("address_line2")
    val addressLine2: String? = null,
    @SerialName("city")
    val city: String? = null,
    @SerialName("state")
    val state: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("email_id")
    val emailId: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("links")
    val links: List<AddressLinkDto>
)

@Serializable
data class AddressLinkDto(
    @SerialName("link_doctype")
    val linkDoctype: String,
    @SerialName("link_name")
    val linkName: String
)

@Serializable
data class ContactCreateDto(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("email_id")
    val emailId: String? = null,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("links")
    val links: List<ContactLinkDto>
)

@Serializable
data class ContactLinkDto(
    @SerialName("link_doctype")
    val linkDoctype: String,
    @SerialName("link_name")
    val linkName: String
)
