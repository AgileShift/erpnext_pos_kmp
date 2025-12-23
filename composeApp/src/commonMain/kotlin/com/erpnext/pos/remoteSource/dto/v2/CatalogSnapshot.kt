package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.localSource.entities.v2.ItemGroupEntity

data class CatalogSnapshot(
    val categories: List<ItemGroupEntity>,
    val items: List<ItemCatalogDto>,
    val prices: Map<String, Float>, // itemId -> rate (for active price list)
    val stock: Map<String, Float>   // itemId -> actualQty (for active warehouse)
)