package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_outbox")
data class CustomerOutboxEntity(
    @PrimaryKey
    @ColumnInfo(name = "local_id")
    val localId: String,
    @ColumnInfo(name = "customer_local_id")
    val customerLocalId: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "status")
    val status: String = "Pending",
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null
)
