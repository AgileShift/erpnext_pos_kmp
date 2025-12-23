package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tax_details",
    foreignKeys = [
        ForeignKey(
            entity = POSClosingEntryEntity::class,
            parentColumns = ["name"],
            childColumns = ["pos_closing_entry"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pos_closing_entry"])]
)
data class TaxDetailsEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "pos_closing_entry")
    var posClosingEntry: String, // FK hacia POSClosingEntryEntity.name
    @ColumnInfo("account_head")
    var accountHead: String,
    var taxAmount: Double,
    var rate: Double
)
