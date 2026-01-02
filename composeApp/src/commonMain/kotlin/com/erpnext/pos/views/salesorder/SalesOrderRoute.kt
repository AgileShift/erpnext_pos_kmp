package com.erpnext.pos.views.salesorder

import SalesOrderScreen
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

@Composable
fun SalesOrderRoute(
    coordinator: SalesOrderCoordinator = rememberSalesOrderCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val navManager: NavigationManager = koinInject()
    val salesFlowStore: SalesFlowContextStore = koinInject()
    val salesContext by salesFlowStore.context.collectAsState()
    val actions = rememberSalesOrderActions(coordinator, navManager, salesFlowStore, salesContext)

    SalesOrderScreen(uiState, actions, salesContext)
}

@Composable
fun rememberSalesOrderActions(
    coordinator: SalesOrderCoordinator,
    navManager: NavigationManager,
    salesFlowStore: SalesFlowContextStore,
    salesContext: SalesFlowContext?
): SalesOrderAction {
    return remember(coordinator, navManager, salesFlowStore, salesContext) {
        SalesOrderAction(
            onBack = coordinator::onBack,
            onRefresh = coordinator::onRefresh,
            onCreateDeliveryNote = { sourceId ->
                salesFlowStore.set(
                    (salesContext ?: SalesFlowContext()).withSource(
                        SalesFlowSource.SalesOrder,
                        sourceId
                    )
                )
                navManager.navigateTo(NavRoute.DeliveryNote)
            },
            onCreateInvoice = { sourceId ->
                salesFlowStore.set(
                    (salesContext ?: SalesFlowContext()).withSource(
                        SalesFlowSource.SalesOrder,
                        sourceId
                    )
                )
                navManager.navigateTo(NavRoute.Billing)
            }
        )
    }
}
