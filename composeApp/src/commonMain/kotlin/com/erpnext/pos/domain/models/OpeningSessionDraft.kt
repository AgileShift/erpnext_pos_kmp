package com.erpnext.pos.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class DenominationCount(
    val value: Double,
    val count: Int
)

@Serializable
data class OpeningSessionDraft(
    val posProfileId: String,
    val user: String,
    val createdAtEpochMillis: Long,
    val openingCashByCurrency: OpeningCashByCurrency,
    val denominationCounts: Map<CurrencyCode, List<DenominationCount>>
)
