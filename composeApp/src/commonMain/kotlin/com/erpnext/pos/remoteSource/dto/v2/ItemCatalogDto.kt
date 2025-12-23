package com.erpnext.pos.remoteSource.dto.v2

data class ItemCatalogDto(
    val itemId: String,
    val itemCode: String,
    val itemName: String,
    val itemGroupId: String,
    val imageUrl: String?,
    val salesUom: String,
    val stockUom: String,
    val allowNegativeStock: Boolean,
    val disabled: Boolean
)