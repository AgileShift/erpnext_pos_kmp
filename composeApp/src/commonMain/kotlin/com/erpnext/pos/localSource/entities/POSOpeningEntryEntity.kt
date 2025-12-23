package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.erpnext.pos.remoteSource.dto.IntAsBooleanSerializer
import kotlinx.serialization.Serializable

@Entity(
    tableName = "tab_pos_opening_entry",
    indices = [Index(value = ["pos_profile", "user"])]
)
data class POSOpeningEntryEntity(
    @PrimaryKey
    var name: String,
    @ColumnInfo(name = "pos_profile")
    var posProfile: String,
    var company: String,
    @ColumnInfo(name = "period_start_date")
    var periodStartDate: String,
    @ColumnInfo("posting_date")
    var postingDate: String,
    var user: String? = null,
    @ColumnInfo(name = "pending_sync")
    @Serializable(with = IntAsBooleanSerializer::class)
    var pendingSync: Boolean
)

data class POSOpeningEntryWithDetails(
    @Embedded var openingEntry: POSOpeningEntryEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "id"
    )
    var balances: List<BalanceDetailsEntity>
)

data class POSOpeningWithClosingAndTaxes(
    @Embedded var opening: POSOpeningEntryEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "pos_opening_entry",
        entity = POSClosingEntryEntity::class
    )
    var closingWithTaxes: POSClosingWithTaxes?
)