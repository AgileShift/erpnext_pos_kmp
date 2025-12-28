package com.erpnext.pos.views.billing

import BillingScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingRoute(
    coordinator: BillingCoordinator = rememberBillingCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val action = rememberBillingActions(coordinator)

    BillingScreen(uiState, action)
}

@Composable
fun rememberBillingActions(coordinator: BillingCoordinator): BillingAction {
    return remember(coordinator) {
        BillingAction(
            onCustomerSearchQueryChange = coordinator::onCustomerSearchQueryChange,
            onCustomerSelected = coordinator::onCustomerSelected,
            onProductSearchQueryChange = coordinator::onProductSearchQueryChange,
            onProductAdded = coordinator::onProductAdded,
            onQuantityChanged = coordinator::onQuantityChanged,
            onRemoveItem = coordinator::onRemoveItem,
            onAddPaymentLine = coordinator::onAddPaymentLine,
            onRemovePaymentLine = coordinator::onRemovePaymentLine,
            onFinalizeSale = coordinator::onFinalizeSale,
            onCreditSaleChanged = coordinator::onCreditSaleChanged,
            onPaymentTermSelected = coordinator::onPaymentTermSelected,
            onDiscountCodeChanged = coordinator::onDiscountCodeChanged,
            onManualDiscountAmountChanged = coordinator::onManualDiscountAmountChanged,
            onManualDiscountPercentChanged = coordinator::onManualDiscountPercentChanged,
            onDeliveryChargeSelected = coordinator::onDeliveryChargeSelected,
            onPaymentCurrencySelected = coordinator::onPaymentCurrencySelected,
            onClearSuccessMessage = coordinator::onClearSuccessMessage,
            onBack = coordinator::onBack
        )
    }
}
