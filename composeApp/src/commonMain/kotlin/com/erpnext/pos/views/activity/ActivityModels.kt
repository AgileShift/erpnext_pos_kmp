package com.erpnext.pos.views.activity

import kotlinx.serialization.Serializable

@Serializable
enum class ActivityPriority {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
enum class ActivityChangeType {
    NONE,
    CREATED,
    UPDATED
}

@Serializable
data class ActivityEntry(
    val id: String,
    val title: String,
    val message: String,
    val priority: ActivityPriority = ActivityPriority.MEDIUM,
    val changeType: ActivityChangeType = ActivityChangeType.NONE,
    val createdAt: Long,
    val readAt: Long? = null
) {
    val isRead: Boolean get() = readAt != null
}

data class ActivityEvent(
    val id: String,
    val title: String,
    val message: String,
    val priority: ActivityPriority = ActivityPriority.MEDIUM,
    val changeType: ActivityChangeType = ActivityChangeType.NONE,
    val triggerSync: Boolean = false,
    val showSnackbar: Boolean? = null
)

