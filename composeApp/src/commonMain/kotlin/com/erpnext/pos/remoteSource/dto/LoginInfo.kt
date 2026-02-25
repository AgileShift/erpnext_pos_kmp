package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginInfo(
    val url: String,
    @SerialName("default_redirect_uri")
    val redirectUrl: String,
    val clientId: String,
    val company: String,
    val scopes: List<String>,
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false
)
