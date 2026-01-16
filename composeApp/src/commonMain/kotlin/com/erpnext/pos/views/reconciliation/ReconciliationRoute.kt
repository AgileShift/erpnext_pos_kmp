package com.erpnext.pos.views.reconciliation

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
fun ReconciliationRoute(
    viewModel: ReconciliationViewModel = koinInject(),
    navManager: NavigationManager = koinInject()
) {
    val state by viewModel.stateFlow.collectAsState()
    val actions = rememberReconciliationActions(navManager)

    ReconciliationScreen(state, actions)
}

@Composable
fun rememberReconciliationActions(navManager: NavigationManager): ReconciliationAction {
    return remember(navManager) {
        ReconciliationAction(onBack = { navManager.navigateTo(NavRoute.NavigateUp) })
    }
}
