package com.erpnext.pos.localSource.entities

import androidx.room.Entity

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    var id: String,
    var entityType: String,
    var payload: String,
    var createdAt: Long
)