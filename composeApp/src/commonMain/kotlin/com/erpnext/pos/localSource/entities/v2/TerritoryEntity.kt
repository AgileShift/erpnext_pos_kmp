package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "territories",
    primaryKeys = ["instanceId", "companyId", "territoryId"]
)
data class TerritoryEntity(
    val territoryId: String, // Territory.name
    val territoryName: String,
    val isGroup: Boolean,
    val parentTerritoryId: String?,
    // Per your instance: Territory.territory_manager is a Link to Sales Person
    val territoryManagerSalesPersonId: String?
) : BaseEntity()
