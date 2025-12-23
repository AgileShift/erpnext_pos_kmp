package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "tabCashbox",
    indices = [Index(
        value = ["posProfile", "user", "openingEntryId", "closingEntryId"],
        unique = true
    )]
)
data class CashboxEntity(
    @PrimaryKey(autoGenerate = true)
    var localId: Long = 0,
    var openingEntryId: String? = null,
    var closingEntryId: String? = null,
    var posProfile: String,
    var company: String,
    var periodStartDate: String,
    var periodEndDate: String? = null,
    var user: String,
    var status: Boolean,
    var pendingSync: Boolean = true // Flag para sync (true si no synced)
)

@Entity(
    tableName = "balance_details",
    foreignKeys = [
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["localId"],
            childColumns = ["cashbox_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = POSOpeningEntryEntity::class,
            parentColumns = ["name"],
            childColumns = ["pos_opening_entry"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = POSClosingEntryEntity::class,
            parentColumns = ["name"],
            childColumns = ["pos_closing_entry"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cashbox_id"]),
        Index(value = ["pos_opening_entry"]),
        Index(value = ["pos_closing_entry"])
    ]
)
data class BalanceDetailsEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "cashbox_id")
    var cashboxId: Long,

    @ColumnInfo(name = "pos_opening_entry")
    var posOpeningEntry: String? = null,

    @ColumnInfo(name = "pos_closing_entry")
    var posClosingEntry: String? = null,

    @ColumnInfo(name = "mode_of_payment")
    var modeOfPayment: String,

    @ColumnInfo(name = "opening_amount")
    var openingAmount: Double,

    @ColumnInfo(name = "closing_amount")
    var closingAmount: Double? = null
)

data class CashboxWithDetails(
    @Embedded var cashbox: CashboxEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "cashbox_id"
    )
    var details: List<BalanceDetailsEntity>
)