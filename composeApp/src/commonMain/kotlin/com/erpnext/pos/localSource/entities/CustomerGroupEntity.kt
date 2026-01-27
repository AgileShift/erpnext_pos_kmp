package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabCustomerGroup")
data class CustomerGroupEntity(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "customer_group_name")
    val customerGroupName: String? = null,
    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,
    @ColumnInfo(name = "parent_customer_group")
    val parentCustomerGroup: String? = null,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)
