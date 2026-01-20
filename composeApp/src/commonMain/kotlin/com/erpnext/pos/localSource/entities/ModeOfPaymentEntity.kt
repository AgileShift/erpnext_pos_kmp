package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(tableName = "tabModeOfPayment")
data class ModeOfPaymentEntity(

    @PrimaryKey
    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "mode_of_payment")
    var modeOfPayment: String, // e.g. "Cash", "Card", "Bank Transfer"

    @ColumnInfo(name = "company", defaultValue = "")
    var company: String,

    @ColumnInfo(name = "type")
    var type: String = "Cash", // "Cash" | "Bank" | "Card" | "Wallet" etc.

    @ColumnInfo(name = "enabled")
    var enabled: Boolean = true,

    @ColumnInfo(name = "currency")
    var currency: String? = null, // can be null if multi-currency not enforced

    @ColumnInfo(name = "account")
    var account: String? = null,  // GL Account in ERPNext

    @ColumnInfo(name = "last_synced_at", defaultValue = "0")
    override var lastSyncedAt: Long? = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity
