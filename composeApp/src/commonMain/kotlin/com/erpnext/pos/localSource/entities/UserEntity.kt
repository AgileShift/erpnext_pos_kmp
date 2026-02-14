package com.erpnext.pos.localSource.entities

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "tabUser")
data class UserEntity(
    @ColumnInfo(name = "user")
    var name: String,
    @ColumnInfo(name = "first_name")
    var firstName: String,
    @ColumnInfo(name = "last_name")
    var lastName: String?,
    @ColumnInfo(name = "username")
    var username: String?,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "email")
    var email: String,
    @ColumnInfo(name = "image")
    var image: String? = null,
    @ColumnInfo(name = "language")
    var language: String?,
    @ColumnInfo(name = "time_zone")
    var timeZone: String? = null,
    @ColumnInfo(name = "full_name")
    var fullName: String? = null,
    @ColumnInfo(name = "enabled")
    var enabled: Boolean
)
