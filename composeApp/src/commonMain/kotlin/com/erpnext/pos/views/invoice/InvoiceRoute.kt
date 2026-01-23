package com.erpnext.pos.views.invoice

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceRoute(
    coordinator: InvoiceCoordinator = rememberInvoiceCoordinator()
) {
    val actions = rememberInvoiceActions(coordinator)
    InvoiceListScreen(actions)
}

@Composable
fun rememberInvoiceActions(coordinator: InvoiceCoordinator): InvoiceAction {
    return remember(coordinator) {
        InvoiceAction(
            onSearchQueryChanged = coordinator::onSearchQueryChanged,
            onItemClick = coordinator::onItemClick,
            goToBilling = coordinator::goToBilling,
            onRefresh = coordinator::onRefresh,
            onPrint = coordinator::onPrint,
            onDateSelected = coordinator::onDateSelected,
            getInvoices = coordinator::getInvoices,
            onInvoiceCancelRequested = coordinator::onInvoiceCancelRequested,
            feedbackMessage = coordinator.feedbackMessage(),
            onFeedbackCleared = coordinator::clearFeedbackMessage
        )
    }
}
