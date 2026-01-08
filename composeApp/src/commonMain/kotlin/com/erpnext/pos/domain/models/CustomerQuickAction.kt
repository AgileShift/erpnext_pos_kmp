package com.erpnext.pos.domain.models

import androidx.compose.ui.graphics.vector.ImageVector

data class CustomerQuickAction(
    val type: CustomerQuickActionType,
    val label: String,
    val icon: ImageVector
)
