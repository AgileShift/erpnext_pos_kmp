package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.erpnext.pos.remoteSource.dto.IntAsBooleanSerializer
import kotlinx.serialization.Serializable

@Entity(
    tableName = "tab_pos_closing_entry",
    foreignKeys = [
        ForeignKey(
            entity = POSOpeningEntryEntity::class,
            parentColumns = ["name"],
            childColumns = ["pos_opening_entry"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pos_opening_entry"])]
)
data class POSClosingEntryEntity(
    @PrimaryKey
    var name: String,
    @ColumnInfo(name = "pos_profile")
    var posProfile: String,
    @ColumnInfo(name = "pos_opening_entry")
    var posOpeningEntry: String,
    var user: String,
    @ColumnInfo(name = "period_start_date")
    var periodStartDate: String,
    @ColumnInfo(name = "period_end_date")
    var periodEndDate: String,
    @ColumnInfo(name = "closing_amount")
    var closingAmount: Double,
    @ColumnInfo(name = "pending_sync")
    @Serializable(with = IntAsBooleanSerializer::class)
    var pendingSync: Boolean
)

data class POSClosingWithTaxes(
    @Embedded var closing: POSClosingEntryEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "pos_closing_entry"
    )
    var taxes: List<TaxDetailsEntity> = emptyList()
)