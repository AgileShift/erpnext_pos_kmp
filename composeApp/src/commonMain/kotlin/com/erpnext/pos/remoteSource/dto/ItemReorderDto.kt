package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemReorderDto(
    @SerialName("parent")
    val itemCode: String,
    @SerialName("warehouse")
    val warehouse: String,
    @SerialName("warehouse_reorder_level")
    val reorderLevel: Double? = null,
    @SerialName("warehouse_reorder_qty")
    val reorderQty: Double? = null
)
