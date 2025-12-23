package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "item_prices",
    primaryKeys = ["instanceId", "companyId", "itemId", "priceList"]
)
data class ItemPriceEntity(
    var itemPriceId: String,
    var itemId: String,
    var priceList: String,
    var rate: Float,
    var currency: String,
) : BaseEntity()