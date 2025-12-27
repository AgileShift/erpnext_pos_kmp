package com.erpnext.pos.views.customer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoute(
    coordinator: CustomerCoordinator = rememberCustomerCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(CustomerState.Loading)
    val navManager: NavigationManager = koinInject()
    val actions = rememberCustomerActions(coordinator, navManager)

    CustomerListScreen(uiState, actions)
}

@Composable
fun rememberCustomerActions(
    coordinator: CustomerCoordinator,
    navManager: NavigationManager
): CustomerAction {
    return remember(coordinator, navManager) {
        CustomerAction(
            fetchAll = coordinator::fetchAll,
            toDetails = coordinator::toDetails,
            onStateSelected = coordinator::onTerritorySelected,
            checkCredit = coordinator::checkCredit,
            onRefresh = coordinator::onRefresh,
            onSearchQueryChanged = coordinator::onSearchQueryChanged,
            onViewPendingInvoices = { navManager.navigateTo(NavRoute.Credits) },
            onCreateQuotation = { navManager.navigateTo(NavRoute.Quotation) },
            onCreateSalesOrder = { navManager.navigateTo(NavRoute.SalesOrder) },
            onCreateDeliveryNote = { navManager.navigateTo(NavRoute.DeliveryNote) },
            onCreateInvoice = { navManager.navigateTo(NavRoute.Billing) },
            onRegisterPayment = { navManager.navigateTo(NavRoute.PaymentEntry()) }
        )
    }
}
