package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

//TODO: Mover
object IntAsBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeInt(if (value) 1 else 0)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return decoder.decodeInt() != 0
    }
}

@Serializable
data class ItemDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("item_group")
    val itemGroup: String,
    @SerialName("description")
    val description: String,
    @SerialName("brand")
    val brand: String? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("disabled")
    @Serializable(with = IntAsBooleanSerializer::class)
    val disabled: Boolean = false,
    @SerialName("stock_uom")
    val stockUom: String,
    @SerialName("standard_rate")
    val standardRate: Double = 0.0,
    @SerialName("is_stock_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isStockItem: Boolean = false,
    @SerialName("is_sales_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isSalesItem: Boolean = true
)

@Serializable
data class BarcodeChild(
    @SerialName("barcode")
    val barcode: String
)