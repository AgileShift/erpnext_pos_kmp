package com.erpnext.pos.localSource.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabUser")
data class UserEntity(
    var name: String,
    var firstName: String,
    var lastName: String?,
    var username: String?,
    @PrimaryKey(autoGenerate = false)
    var email: String,
    var language: String?,
    var enabled: Boolean
)