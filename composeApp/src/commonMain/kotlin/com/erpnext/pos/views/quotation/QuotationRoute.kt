package com.erpnext.pos.views.quotation

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
fun QuotationRoute(
    coordinator: QuotationCoordinator = rememberQuotationCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val navManager: NavigationManager = koinInject()
    val salesFlowStore: SalesFlowContextStore = koinInject()
    val salesContext by salesFlowStore.context.collectAsState()
    val actions = rememberQuotationActions(coordinator, navManager, salesFlowStore, salesContext)

    QuotationScreen(uiState, actions, salesContext)
}

@Composable
fun rememberQuotationActions(
    coordinator: QuotationCoordinator,
    navManager: NavigationManager,
    salesFlowStore: SalesFlowContextStore,
    salesContext: SalesFlowContext?
): QuotationAction {
    return remember(coordinator, navManager, salesFlowStore, salesContext) {
        QuotationAction(
            onBack = coordinator::onBack,
            onRefresh = coordinator::onRefresh,
            onCreateSalesOrder = { sourceId ->
                salesFlowStore.set(
                    (salesContext ?: SalesFlowContext()).withSource(
                        SalesFlowSource.Quotation,
                        sourceId
                    )
                )
                navManager.navigateTo(NavRoute.SalesOrder)
            },
            onCreateInvoice = { sourceId ->
                salesFlowStore.set(
                    (salesContext ?: SalesFlowContext()).withSource(
                        SalesFlowSource.Quotation,
                        sourceId
                    )
                )
                navManager.navigateTo(NavRoute.Billing)
            }
        )
    }
}
