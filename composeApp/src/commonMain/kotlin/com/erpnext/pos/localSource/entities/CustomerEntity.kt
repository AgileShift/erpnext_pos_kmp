package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(tableName = "customers")
data class CustomerEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = false) var name: String,
    var customerName: String,
    var territory: String?,
    var email: String?,
    var mobileNo: String?,
    var customerType: String,
    var creditLimit: Double? = null,
    var currentBalance: Double? = 0.0,
    var totalPendingAmount: Double? = 0.0,  // Sum outstanding_amount
    var pendingInvoicesCount: Int? = 0,
    var availableCredit: Double? = 0.0,
    var image: String? = null,
    var address: String? = null,
    var state: String? = null,
    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    override var lastSyncedAt: Long? = Clock.System.now().toEpochMilliseconds() // Formatted
) : SyncableEntity
