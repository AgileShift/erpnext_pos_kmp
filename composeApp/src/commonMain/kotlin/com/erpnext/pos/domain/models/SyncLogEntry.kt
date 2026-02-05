package com.erpnext.pos.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class SyncLogStatus {
    SUCCESS,
    PARTIAL,
    ERROR,
    CANCELED
}

@Serializable
data class SyncLogEntry(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMs: Long,
    val totalSteps: Int,
    val failedSteps: List<String>,
    val status: SyncLogStatus,
    val message: String
)
