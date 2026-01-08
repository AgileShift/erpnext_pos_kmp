package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "companies",
    primaryKeys = ["company"]
)
data class CompanyEntity(
    @ColumnInfo(name = "company")
    val companyName: String,
    @ColumnInfo(name = "default_currency")
    var defaultCurrency: String,
    var country: String? = null,
    @ColumnInfo(name = "tax_id")
    var taxId: String? = null,
)