package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "inventory_bins",
    primaryKeys = ["instanceId", "companyId", "warehouseId", "itemId"]
)
data class InventoryBinEntity(
    var itemId: String,
    var warehouseId: String,
    var actualQty: Float,
    var projectedQty: Float? = null,
    var reservedQty: Float? = null
) : BaseEntity()
