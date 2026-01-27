package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(tableName = "tabItem")
data class ItemEntity(
    @PrimaryKey(autoGenerate = false)
    var itemCode: String = "",
    var actualQty: Double = 0.0,
    var name: String = "",
    var description: String,
    var barcode: String = "",
    var image: String? = null,
    var itemGroup: String = "",
    var brand: String? = null,
    var price: Double = 0.0,
    @ColumnInfo(name = "valuation_rate", defaultValue = "0")
    var valuationRate: Double? = 0.0,
    var discount: Double = 0.0,
    var isService: Boolean = false,
    var isStocked: Boolean = false,
    var stockUom: String,
    var currency: String,
    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    override var lastSyncedAt: Long? = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity
