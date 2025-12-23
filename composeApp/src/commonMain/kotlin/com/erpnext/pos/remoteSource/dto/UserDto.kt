package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("name")
    val name: String,
    @SerialName("username")
    val username: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("email")
    val email: String,
    @SerialName("language")
    val language: String,
    @Serializable(with = IntAsBooleanSerializer::class)
    @SerialName("enabled")
    val enabled: Boolean
)