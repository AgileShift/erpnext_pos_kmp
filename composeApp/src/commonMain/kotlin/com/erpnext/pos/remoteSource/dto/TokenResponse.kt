package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val id_token: String? = null,
    val scope: String? = null,
)