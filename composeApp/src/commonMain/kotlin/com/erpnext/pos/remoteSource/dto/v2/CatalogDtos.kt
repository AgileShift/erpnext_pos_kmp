package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.remoteSource.dto.IntAsBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("item_group")
    val itemGroup: String,
    @SerialName("brand")
    val brand: String? = null,
    @SerialName("image")
    val imageUrl: String? = null,
    @SerialName("disabled")
    @Serializable(with = IntAsBooleanSerializer::class)
    val disabled: Boolean = false,
    @SerialName("stock_uom")
    val stockUom: String? = null,
    @SerialName("sales_uom")
    val salesUom: String? = null,
    @SerialName("is_stock_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isStockItem: Boolean = false,
    @SerialName("allow_negative_stock")
    @Serializable(with = IntAsBooleanSerializer::class)
    val allowNegativeStock: Boolean = false
)

@Serializable
data class ItemGroupDto(
    @SerialName("name")
    val itemGroupId: String,
    @SerialName("item_group_name")
    val itemGroupName: String? = null,
    @SerialName("parent_item_group")
    val parentItemGroupId: String? = null,
    @SerialName("is_group")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isGroup: Boolean = false
)

@Serializable
data class ItemPriceDto(
    @SerialName("name")
    val itemPriceId: String,
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("price_list")
    val priceList: String,
    @SerialName("price_list_rate")
    val priceListRate: Double,
    @SerialName("currency")
    val currency: String
)


@Serializable
data class BinDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("warehouse")
    val warehouse: String,
    @SerialName("actual_qty")
    val actualQty: Double,
    @SerialName("projected_qty")
    val projectedQty: Double? = null,
    @SerialName("reserved_qty")
    val reservedQty: Double? = null
)
