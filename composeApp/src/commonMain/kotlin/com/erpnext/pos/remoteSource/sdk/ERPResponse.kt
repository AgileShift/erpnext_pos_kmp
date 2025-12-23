package com.erpnext.pos.remoteSource.sdk

import kotlinx.serialization.Serializable

// -----------------------------
// Modelos del wrapper
// -----------------------------
@Serializable
private data class ERPResponse<T>(
    val data: T
)
