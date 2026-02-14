package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.remoteSource.dto.UserDto

fun UserDto.toEntity(): UserEntity {
    val normalizedName = this.name
    val normalizedUsername = this.username?.takeIf { it.isNotBlank() } ?: normalizedName
    val normalizedFirstName = this.firstName?.takeIf { it.isNotBlank() }
        ?: this.fullName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
        ?: normalizedUsername
    val normalizedEmail = this.email?.takeIf { it.isNotBlank() } ?: normalizedUsername
    return UserEntity(
        name = normalizedName,
        firstName = normalizedFirstName,
        lastName = this.lastName,
        username = normalizedUsername,
        email = normalizedEmail,
        image = this.image,
        language = this.language,
        timeZone = this.timeZone,
        fullName = this.fullName,
        enabled = this.enabled
    )
}
