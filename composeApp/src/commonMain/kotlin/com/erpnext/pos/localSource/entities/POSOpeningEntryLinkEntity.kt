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
    tableName = "tab_pos_opening_entry_link",
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
            childColumns = ["local_opening_entry_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cashbox_id"], unique = true),
        Index(value = ["local_opening_entry_name"])
    ]
)
@Serializable
data class POSOpeningEntryLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "cashbox_id")
    val cashboxId: Long,
    @ColumnInfo(name = "local_opening_entry_name")
    val localOpeningEntryName: String,
    @ColumnInfo(name = "remote_opening_entry_name")
    val remoteOpeningEntryName: String? = null,
    @ColumnInfo(name = "remote_closing_entry_name")
    val remoteClosingEntryName: String? = null,
    @ColumnInfo(name = "pending_sync")
    @Serializable(with = IntAsBooleanSerializer::class)
    val pendingSync: Boolean = true
)

data class PendingOpeningEntrySync(
    @Embedded val link: POSOpeningEntryLinkEntity,
    @Relation(
        parentColumn = "cashbox_id",
        entityColumn = "localId"
    )
    val cashbox: CashboxEntity,
    @Relation(
        parentColumn = "cashbox_id",
        entityColumn = "cashbox_id"
    )
    val balanceDetails: List<BalanceDetailsEntity>,
    @Relation(
        parentColumn = "local_opening_entry_name",
        entityColumn = "name"
    )
    val openingEntry: POSOpeningEntryEntity
)
