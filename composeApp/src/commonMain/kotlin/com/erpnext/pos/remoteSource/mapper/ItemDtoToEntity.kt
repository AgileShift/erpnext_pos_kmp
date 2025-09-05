package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.remoteSource.dto.ItemDto

fun List<ItemDto>.toEntity(): List<ItemEntity> {
    return this.map { it.toEntity() }
}

fun ItemDto.toEntity(): ItemEntity {
    return ItemEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        barcode = this.barcode,
        image = this.image,
        price = this.price,
        discount = this.discount,
        isService = this.isService,
        isStocked = this.isStocked,
        uom = this.uom
    )
}