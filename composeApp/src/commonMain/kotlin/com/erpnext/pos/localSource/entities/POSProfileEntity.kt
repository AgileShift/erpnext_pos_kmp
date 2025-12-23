package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(tableName = "tabPosProfile")
data class POSProfileEntity(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "profile_name") var profileName: String,

    @ColumnInfo(name = "warehouse") var warehouse: String,

    @ColumnInfo(name = "route") var route: String? = null,

    @ColumnInfo(name = "country") var country: String,

    @ColumnInfo(name = "company") var company: String,

    @ColumnInfo(name = "currency") var currency: String,

    @ColumnInfo("income_account") var incomeAccount: String,

    @ColumnInfo("expense_account") var expenseAccount: String,

    @ColumnInfo("branch") var branch: String,

    @ColumnInfo("apply_discount_on") var applyDiscountOn: String,

    @ColumnInfo("cost_center") var costCenter: String,

    @ColumnInfo("selling_price_list") var sellingPriceList: String,

    @ColumnInfo(name = "active") var active: Boolean? = false,

    @ColumnInfo(name = "user") var user: String? = null,

    @ColumnInfo(name = "last_synced_at")
    override var lastSyncedAt: Long? = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

data class POSProfileWithPaymentsInfo(
    @Embedded var profile: POSProfileEntity, @Relation(
        parentColumn = "profile_name", entityColumn = "profile_id"
    ) var payments: List<POSInvoicePaymentEntity>
)