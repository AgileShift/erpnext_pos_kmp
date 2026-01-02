package com.erpnext.pos.views.customer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.views.salesflow.SalesFlowContext
import com.erpnext.pos.views.salesflow.SalesFlowContextStore
import com.erpnext.pos.views.salesflow.SalesFlowSource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoute(
    coordinator: CustomerCoordinator = rememberCustomerCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(CustomerState.Loading)
    val invoicesState by coordinator.invoicesState.collectAsState()
    val paymentState by coordinator.paymentState.collectAsState()
    val navManager: NavigationManager = koinInject()
    val salesFlowStore: SalesFlowContextStore = koinInject()
    val actions = rememberCustomerActions(coordinator, navManager, salesFlowStore)

    CustomerListScreen(uiState, invoicesState, paymentState, actions)
}

@Composable
fun rememberCustomerActions(
    coordinator: CustomerCoordinator,
    navManager: NavigationManager,
    salesFlowStore: SalesFlowContextStore
): CustomerAction {
    return remember(coordinator, navManager, salesFlowStore) {
        CustomerAction(
            fetchAll = coordinator::fetchAll,
            toDetails = coordinator::toDetails,
            onStateSelected = coordinator::onTerritorySelected,
            checkCredit = coordinator::checkCredit,
            onRefresh = coordinator::onRefresh,
            onSearchQueryChanged = coordinator::onSearchQueryChanged,
            onViewPendingInvoices = { coordinator.loadOutstandingInvoices(it.name) },
            onCreateQuotation = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.Quotation)
            },
            onCreateSalesOrder = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.SalesOrder)
            },
            onCreateDeliveryNote = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.DeliveryNote)
            },
            onCreateInvoice = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.Billing)
            },
            onRegisterPayment = { coordinator.loadOutstandingInvoices(it.name) },
            loadOutstandingInvoices = { coordinator.loadOutstandingInvoices(it.name) },
            clearOutstandingInvoices = coordinator::clearOutstandingInvoices,
            registerPayment = coordinator::registerPayment
        )
    }
}
