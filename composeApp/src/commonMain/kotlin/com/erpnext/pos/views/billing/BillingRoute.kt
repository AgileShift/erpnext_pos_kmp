package com.erpnext.pos.views.billing

import androidx.paging.PagingData
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erpnext.pos.utils.view.SnackbarController
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingRoute(
    coordinator: BillingCoordinator = rememberBillingCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val productsPagingFlow by coordinator.productsPagingFlow.collectAsState(flowOf(PagingData.empty()))
    val action = rememberBillingActions(coordinator)
    val snackbar = koinInject<SnackbarController>()

    BillingScreen(uiState, productsPagingFlow, action, snackbar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingLabRoute(
    backStackEntry: NavBackStackEntry,
    coordinator: BillingCoordinator = rememberBillingLabCoordinator(backStackEntry)
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val productsPagingFlow by coordinator.productsPagingFlow.collectAsState(flowOf(PagingData.empty()))
    val action = rememberBillingActions(coordinator)
    val snackbar = koinInject<SnackbarController>()

    BillingScreen(uiState, productsPagingFlow, action, snackbar)
}

@Composable
fun rememberBillingActions(coordinator: BillingCoordinator): BillingAction {
    return remember(coordinator) {
        BillingAction(
            onCustomerSearchQueryChange = coordinator::onCustomerSearchQueryChange,
            onCustomerSelected = coordinator::onCustomerSelected,
            onProductSearchQueryChange = coordinator::onProductSearchQueryChange,
            onProductCategorySelected = coordinator::onProductCategorySelected,
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
            onResetSale = coordinator::onResetSale,
            onBack = coordinator::onBack,
            onLinkSource = coordinator::onLinkSource,
            onClearSource = coordinator::onClearSource,
            onLoadSourceDocuments = coordinator::onLoadSourceDocuments,
            onSyncExchangeRates = coordinator::onSyncExchangeRates
        )
    }
}
