package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabSupplier")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = false)
    var name: String,
    @ColumnInfo(name = "supplier_name")
    var supplierName: String? = null,
    @ColumnInfo(name = "supplier_group")
    var supplierGroup: String? = null,
    @ColumnInfo(name = "supplier_type")
    var supplierType: String? = null,
    @ColumnInfo(name = "default_currency")
    var defaultCurrency: String? = null,
    @ColumnInfo(name = "disabled")
    var disabled: Int? = null,
    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    var lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
