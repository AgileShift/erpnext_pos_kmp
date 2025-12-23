package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(tableName = "routes")
data class RouteEntity(
    var name: String,
    var territoryName: String,
    var isGroup: Boolean = false,
    var parentTerritoryId: String? = null,
    var territoryManager: String? = null,
) : BaseEntity()