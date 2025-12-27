package com.erpnext.pos.views.deliverynote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun DeliveryNoteRoute(
    coordinator: DeliveryNoteCoordinator = rememberDeliveryNoteCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val actions = rememberDeliveryNoteActions(coordinator)

    DeliveryNoteScreen(uiState, actions)
}

@Composable
fun rememberDeliveryNoteActions(coordinator: DeliveryNoteCoordinator): DeliveryNoteAction {
    return remember(coordinator) {
        DeliveryNoteAction(
            onRefresh = coordinator::onRefresh
        )
    }
}
