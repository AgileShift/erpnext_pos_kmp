package com.erpnext.pos.views.inventory

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRoute(
    coordinator: InventoryCoordinator = rememberInventoryCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(InventoryState.Loading)
    val actions = rememberInventoryActions(coordinator)

    InventoryScreen(uiState, actions)
}

@Composable
fun rememberInventoryActions(coordinator: InventoryCoordinator): InventoryAction {
    return remember(coordinator) {
        InventoryAction(
            fetchAll = coordinator::fetchInventory,
            getDetails = coordinator::getItemDetails,
            refresh = coordinator::refresh,
            onError = coordinator::onError
        )
    }
}