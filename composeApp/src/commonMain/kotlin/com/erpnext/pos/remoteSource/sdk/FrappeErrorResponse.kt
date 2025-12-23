package com.erpnext.pos.remoteSource.sdk

import kotlinx.serialization.Serializable

// -----------------------------
// Respuesta de error y excepción
// -----------------------------
@Serializable
data class FrappeErrorResponse(
    val exception: String? = null,
    val _server_messages: String? = null,
)

class FrappeException(message: String, val errorResponse: FrappeErrorResponse? = null) :
    Exception(message)