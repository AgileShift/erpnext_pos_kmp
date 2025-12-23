package com.erpnext.pos.localSource.entities

data class ItemHeader(
    val itemCode: String,
    val name: String,
    val price: Double,
    val image: String?,
    val stockUom: String,
    val actualQty: Double
)
