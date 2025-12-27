package com.erpnext.pos.views.paymententry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun PaymentEntryRoute(
    invoiceId: String?,
    coordinator: PaymentEntryCoordinator = rememberPaymentEntryCoordinator()
) {
    val uiState by coordinator.state.collectAsState()

    LaunchedEffect(invoiceId) {
        coordinator.setInvoiceId(invoiceId)
    }
    val actions = rememberPaymentEntryActions(coordinator)

    PaymentEntryScreen(uiState, actions)
}

@Composable
fun rememberPaymentEntryActions(coordinator: PaymentEntryCoordinator): PaymentEntryAction {
    return remember(coordinator) {
        PaymentEntryAction(
            onInvoiceIdChanged = coordinator::onInvoiceIdChanged,
            onModeOfPaymentChanged = coordinator::onModeOfPaymentChanged,
            onAmountChanged = coordinator::onAmountChanged,
            onSubmit = coordinator::onSubmit,
            onBack = coordinator::onBack
        )
    }
}
