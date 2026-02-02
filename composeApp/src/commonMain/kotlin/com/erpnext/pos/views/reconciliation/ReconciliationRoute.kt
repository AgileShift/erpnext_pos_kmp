package com.erpnext.pos.views.reconciliation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.erpnext.pos.navigation.NavigationManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationRoute(
    mode: ReconciliationMode,
    viewModel: ReconciliationViewModel = koinInject(),
    navManager: NavigationManager = koinInject()
) {
    val state by viewModel.stateFlow.collectAsState()
    val closeState by viewModel.closeState.collectAsState()
    val actions = rememberReconciliationActions(navManager, viewModel)

    LaunchedEffect(mode) {
        viewModel.reload()
    }

    LaunchedEffect(closeState.isClosed, mode, state) {
        if (mode == ReconciliationMode.Close &&
            (closeState.isClosed || (state is ReconciliationState.Empty && !closeState.isClosing))
        ) {
            navManager.navigateTo(com.erpnext.pos.navigation.NavRoute.Home)
        }
    }

    ReconciliationScreen(state, mode, closeState, actions)
}

@Composable
fun rememberReconciliationActions(
    navManager: NavigationManager,
    viewModel: ReconciliationViewModel
): ReconciliationAction {
    return remember(navManager, viewModel) {
        ReconciliationAction(
            onBack = { navManager.navigateTo(com.erpnext.pos.navigation.NavRoute.NavigateUp) },
            onConfirmClose = viewModel::closeCashbox,
            onSaveDraft = {},
            onReload = viewModel::reload
        )
    }
}
