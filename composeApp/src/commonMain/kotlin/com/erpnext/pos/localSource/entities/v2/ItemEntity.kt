package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "items",
    primaryKeys = ["instanceId", "companyId", "itemId"]
)
data class ItemEntity(
    var itemId: String,
    var itemCode: String,
    var itemName: String,
    var itemGroup: String,
    var brand: String? = null,
    var stockUom: String? = "Unit",
    var salesUom: String? = "Unit",
    var isStockItem: Boolean = false,
    var allowNegativeStock: Boolean = false,
    var imageUrl: String? = null,
    var disabled: Boolean = false
) : BaseEntity()