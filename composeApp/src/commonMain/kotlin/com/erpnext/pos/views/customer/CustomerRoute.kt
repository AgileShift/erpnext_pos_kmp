package com.erpnext.pos.views.customer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoute(
    coordinator: CustomerCoordinator = rememberCustomerCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(CustomerState.Loading)
    val actions = rememberCustomerActions(coordinator)

    CustomerListScreen(uiState, actions)
}

@Composable
fun rememberCustomerActions(coordinator: CustomerCoordinator): CustomerAction {
    return remember(coordinator) {
        CustomerAction(
            fetchAll = coordinator::fetchAll,
            toDetails = coordinator::toDetails,
            onStateSelected = coordinator::onTerritorySelected,
            checkCredit = coordinator::checkCredit,
            onRefresh = coordinator::onRefresh,
            onSearchQueryChanged = coordinator::onSearchQueryChanged
        )
    }
}