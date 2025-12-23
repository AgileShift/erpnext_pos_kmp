package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.remoteSource.dto.UserDto

fun UserDto.toEntity(): UserEntity {
    return UserEntity(
        name = this.name,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        language = this.language,
        email = this.email,
        enabled = this.enabled
    )
}