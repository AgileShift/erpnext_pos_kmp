package com.erpnext.pos.localSource.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabCategory")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = false)
    var name: String
)