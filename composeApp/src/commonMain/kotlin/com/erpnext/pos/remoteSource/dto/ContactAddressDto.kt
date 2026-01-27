package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkRefDto(
    @SerialName("link_doctype")
    val linkDoctype: String? = null,
    @SerialName("link_name")
    val linkName: String? = null
)

@Serializable
data class ContactListDto(
    @SerialName("name")
    val name: String,
    @SerialName("email_id")
    val emailId: String? = null,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    val links: List<LinkRefDto> = emptyList()
)

@Serializable
data class AddressListDto(
    @SerialName("name")
    val name: String,
    @SerialName("address_title")
    val addressTitle: String? = null,
    @SerialName("address_type")
    val addressType: String? = null,
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
    val links: List<LinkRefDto> = emptyList()
)

@Serializable
data class ContactUpdateDto(
    @SerialName("email_id")
    val emailId: String? = null,
    @SerialName("mobile_no")
    val mobileNo: String? = null,
    @SerialName("phone")
    val phone: String? = null
)

@Serializable
data class AddressUpdateDto(
    @SerialName("address_title")
    val addressTitle: String? = null,
    @SerialName("address_type")
    val addressType: String? = null,
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
    val phone: String? = null
)
