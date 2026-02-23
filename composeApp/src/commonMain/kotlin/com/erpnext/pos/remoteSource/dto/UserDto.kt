package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("user")
    val name: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("language")
    val language: String? = null,
    @SerialName("time_zone")
    val timeZone: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @Serializable(with = IntAsBooleanSerializer::class)
    @SerialName("enabled")
    val enabled: Boolean = true
)
