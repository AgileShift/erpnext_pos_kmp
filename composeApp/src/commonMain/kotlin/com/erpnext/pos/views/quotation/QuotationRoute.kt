package com.erpnext.pos.views.quotation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun QuotationRoute(
    coordinator: QuotationCoordinator = rememberQuotationCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val actions = rememberQuotationActions(coordinator)

    QuotationScreen(uiState, actions)
}

@Composable
fun rememberQuotationActions(coordinator: QuotationCoordinator): QuotationAction {
    return remember(coordinator) {
        QuotationAction(
            onRefresh = coordinator::onRefresh
        )
    }
}
