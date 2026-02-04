package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BinDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("warehouse")
    val warehouse: String,
    @SerialName("actual_qty")
    val actualQty: Double,
    @SerialName("reserved_qty")
    val reservedQty: Double? = null,
    @SerialName("projected_qty")
    val projectedQty: Double? = null,
    @SerialName("stock_uom")
    val stockUom: String,
    @SerialName("valuation_rate")
    val valuationRate: Double = 0.0  // Costo, útil para margen en POS
)
