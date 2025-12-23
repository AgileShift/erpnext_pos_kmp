package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.remoteSource.dto.CategoryDto

fun CategoryDto.toEntity(): CategoryEntity {
    return CategoryEntity(
        name = this.name
    )
}