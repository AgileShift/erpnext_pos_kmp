package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "users",
    primaryKeys = ["instanceId", "companyId", "userId"],
    indices = [
        Index("email")
    ]
)
data class UserEntity(
    var userId: String,
    var email: String,
    var fullName: String,
    var enabled: Boolean = true,
    var userType: String
) : BaseEntity()
