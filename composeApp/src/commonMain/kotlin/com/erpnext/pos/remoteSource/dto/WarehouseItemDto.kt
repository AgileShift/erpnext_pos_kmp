package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//TODO: Aca tengo que agregar los campos verdaderos que tengo en
//TODO: ItemDto/ItemEntity para que todo quede tal cual pero con la
//TODO: Combination de Precios y Stock
@Serializable
data class WarehouseItemDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("actual_qty")
    val actualQty: Double,
    @SerialName("price")
    val price: Double,
    val name: String,  // De item_name
    @SerialName("item_group")
    val itemGroup: String,
    val description: String,
    val barcode: String = "",  // Default; no en JSON
    val image: String = "",
    val discount: Double = 0.0,  // Default; no field
    @SerialName("is_service")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isService: Boolean = false,  // Inferido
    @SerialName("is_stocked")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isStocked: Boolean = false,  // De is_stock_item
    @SerialName("stock_uom")
    val stockUom: String,
    val brand: String = "",
    val currency: String = "" // Inferido
)