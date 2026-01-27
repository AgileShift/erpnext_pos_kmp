package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TerritoryDto(
    @SerialName("name")
    val name: String,
    @SerialName("territory_name")
    val territoryName: String? = null,
    @SerialName("is_group")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isGroup: Boolean = false,
    @SerialName("parent_territory")
    val parentTerritory: String? = null
)
