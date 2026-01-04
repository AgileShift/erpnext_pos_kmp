package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.WarehouseItemDto
import kotlin.jvm.JvmName

@JvmName("toEntityItemDto")
fun List<ItemDto>.toEntity(): List<ItemEntity> {
    return this.map { it.toEntity() }
}

@JvmName("toEntityWarehouseItemDto")
fun List<WarehouseItemDto>.toEntity(): List<ItemEntity> {
    return this.map { it.toEntity() }
}

fun WarehouseItemDto.toEntity(): ItemEntity {
    return ItemEntity(
        name = this.name,
        currency = this.currency,
        itemCode = this.itemCode,
        actualQty = this.actualQty,
        itemGroup = this.itemGroup,
        description = this.description,
        barcode = this.barcode,
        image = this.image,
        price = this.price,
        valuationRate = this.valuationRate,
        discount = this.discount,
        isService = this.isService,
        isStocked = this.isStocked,
        stockUom = this.stockUom,
        brand = this.brand,
    )
}

fun ItemDto.toEntity(): ItemEntity {
    return ItemEntity(
        name = this.itemName,
        itemCode = this.itemCode,
        itemGroup = this.itemGroup,
        description = this.description,
        image = this.image,
        stockUom = this.stockUom,
        brand = this.brand,
        currency = ""
    )
}
