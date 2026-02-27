package com.erpnext.pos.views.paymententry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erpnext.pos.navigation.GlobalTopBarState
import com.erpnext.pos.navigation.LocalTopBarController

@Composable
fun PaymentEntryRoute(
    invoiceId: String?,
    entryType: String?,
    coordinator: PaymentEntryCoordinator = rememberPaymentEntryCoordinator()
) {
    val uiState by coordinator.state.collectAsState()
    val topBarController = LocalTopBarController.current
    DisposableEffect(Unit) {
        onDispose {
            coordinator.resetFormState()
            topBarController.reset()
        }
    }

    LaunchedEffect(invoiceId, entryType) {
        coordinator.resetFormState()
        coordinator.setEntryType(PaymentEntryType.from(entryType))
        coordinator.setInvoiceId(invoiceId)
    }
    LaunchedEffect(uiState.entryType) {
        val subtitle = when (uiState.entryType) {
            PaymentEntryType.Pay -> "Gasto"
            PaymentEntryType.InternalTransfer -> "Transferencia Interna"
            PaymentEntryType.Receive -> "Cobro de Factura"
        }
        topBarController.set(
            GlobalTopBarState(
                subtitle = subtitle,
                showBack = false
            )
        )
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
            onTargetModeOfPaymentChanged = coordinator::onTargetModeOfPaymentChanged,
            onSourceAccountChanged = coordinator::onSourceAccountChanged,
            onTargetAccountChanged = coordinator::onTargetAccountChanged,
            onAmountChanged = coordinator::onAmountChanged,
            onConceptChanged = coordinator::onConceptChanged,
            onPartyChanged = coordinator::onPartyChanged,
            onSupplierInvoiceToggled = coordinator::onSupplierInvoiceToggled,
            onReferenceNoChanged = coordinator::onReferenceNoChanged,
            onReferenceDateChanged = coordinator::onReferenceDateChanged,
            onNotesChanged = coordinator::onNotesChanged,
            onSubmit = coordinator::onSubmit,
            onBack = coordinator::onBack
        )
    }
}
