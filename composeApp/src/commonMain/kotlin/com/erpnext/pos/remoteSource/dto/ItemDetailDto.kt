package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDetailDto(
    @SerialName("item_code")
    val itemCode: String? = null,
    @SerialName("actual_qty")
    val actualQty: Double = 0.0,
    @SerialName("rate")
    val price: Double = 0.0,  // Precio calculado con rules
    @SerialName("item_name")
    val name: String = "",
    @SerialName("item_group")
    val itemGroup: String = "",
    @SerialName("description")
    val description: String = "",
    val barcode: String = "",  // Procesado si barcodes
    @SerialName("image")
    val image: String = "",
    val discount: Double = 0.0,  // Inferido o 0
    @Serializable(with = IntAsBooleanSerializer::class)
    val isService: Boolean = false,  // Inferido
    @SerialName("is_stock_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isStocked: Boolean = false,
    @SerialName("stock_uom")
    val stockUom: String = "",
    @SerialName("brand")
    val brand: String = "",
    @SerialName("standard_rate")
    val standardRate: Double = 0.0,  // Fallback si no rate
    @SerialName("is_sales_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isSalesItem: Boolean = true  // Para vendibles
)