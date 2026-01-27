package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabAddress")
data class AddressEntity(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "customer_id")
    val customerId: String? = null,
    @ColumnInfo(name = "address_title")
    val addressTitle: String? = null,
    @ColumnInfo(name = "address_type")
    val addressType: String? = null,
    @ColumnInfo(name = "address_line1")
    val addressLine1: String? = null,
    @ColumnInfo(name = "address_line2")
    val addressLine2: String? = null,
    @ColumnInfo(name = "city")
    val city: String? = null,
    @ColumnInfo(name = "state")
    val state: String? = null,
    @ColumnInfo(name = "country")
    val country: String? = null,
    @ColumnInfo(name = "email_id")
    val emailId: String? = null,
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
