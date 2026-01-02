package com.erpnext.pos.views.deliverynote

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
fun DeliveryNoteRoute(
    coordinator: DeliveryNoteCoordinator = rememberDeliveryNoteCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState()
    val navManager: NavigationManager = koinInject()
    val salesFlowStore: SalesFlowContextStore = koinInject()
    val salesContext by salesFlowStore.context.collectAsState()
    val actions = rememberDeliveryNoteActions(coordinator, navManager, salesFlowStore, salesContext)

    DeliveryNoteScreen(uiState, actions, salesContext)
}

@Composable
fun rememberDeliveryNoteActions(
    coordinator: DeliveryNoteCoordinator,
    navManager: NavigationManager,
    salesFlowStore: SalesFlowContextStore,
    salesContext: SalesFlowContext?
): DeliveryNoteAction {
    return remember(coordinator, navManager, salesFlowStore, salesContext) {
        DeliveryNoteAction(
            onBack = coordinator::onBack,
            onRefresh = coordinator::onRefresh,
            onCreateInvoice = { sourceId ->
                salesFlowStore.set(
                    (salesContext ?: SalesFlowContext()).withSource(
                        SalesFlowSource.DeliveryNote,
                        sourceId
                    )
                )
                navManager.navigateTo(NavRoute.Billing)
            }
        )
    }
}
