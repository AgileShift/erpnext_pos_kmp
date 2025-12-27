package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRateResponse(
    val message: Double
)
