package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabCompanyAccount")
data class CompanyAccountEntity(
    @PrimaryKey(autoGenerate = false)
    var name: String,
    @ColumnInfo(name = "account_name")
    var accountName: String? = null,
    @ColumnInfo(name = "account_type")
    var accountType: String? = null,
    @ColumnInfo(name = "account_currency")
    var accountCurrency: String? = null,
    @ColumnInfo(name = "company")
    var company: String? = null,
    @ColumnInfo(name = "is_group")
    var isGroup: Int? = null,
    @ColumnInfo(name = "disabled")
    var disabled: Int? = null,
    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    var lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
