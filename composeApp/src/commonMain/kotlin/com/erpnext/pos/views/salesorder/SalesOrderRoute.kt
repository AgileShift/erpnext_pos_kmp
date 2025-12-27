package com.erpnext.pos.views.salesorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun SalesOrderRoute(
    coordinator: SalesOrderCoordinator = rememberSalesOrderCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val actions = rememberSalesOrderActions(coordinator)

    SalesOrderScreen(uiState, actions)
}

@Composable
fun rememberSalesOrderActions(coordinator: SalesOrderCoordinator): SalesOrderAction {
    return remember(coordinator) {
        SalesOrderAction(
            onBack = coordinator::onBack,
            onRefresh = coordinator::onRefresh
        )
    }
}
