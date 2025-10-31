package com.erpnext.pos.localSource.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "tabCashbox",
    indices = [Index(value = ["posProfile", "user"], unique = true)]
)
data class CashboxEntity(
    @PrimaryKey(autoGenerate = true)
    var localId: Long = 0,
    var posProfile: String,
    var company: String,
    var periodStartDate: Long,
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
            childColumns = ["cashboxId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BalanceDetailsEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var cashboxId: Long = 0,
    var modeOfPayment: String,
    var openingAmount: Double
)

data class CashboxWithDetails(
    @Embedded var cashbox: CashboxEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "cashboxId"
    )
    var details: List<BalanceDetailsEntity>
)