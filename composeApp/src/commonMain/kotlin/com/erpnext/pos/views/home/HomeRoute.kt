package com.erpnext.pos.views.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    coordinator: HomeCoordinator = rememberHomeCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(HomeState.Loading)
    val actions = rememberHomeActions(coordinator)

    HomeScreen(uiState, actions)
}

@Composable
fun rememberHomeActions(coordinator: HomeCoordinator): HomeAction {
    return remember(coordinator) {
        HomeAction(
            sync = coordinator::sync,
            syncState = coordinator.syncState,
            syncSettings = coordinator.syncSettings,
            homeMetrics = coordinator.homeMetrics,
            initialState = coordinator::initialState,
            loadInitialData = coordinator::loadInitialData,
            onOpenSettings = coordinator::openSettings,
            onOpenReconciliation = coordinator::openReconciliation,
            onCloseCashbox = coordinator::openCloseCashbox,
            onLogout = coordinator::logout,
            onError = coordinator::onError,
            openCashbox = coordinator::openCashbox,
            onPosSelected = coordinator::onPosSelected,
            closeCashbox = coordinator::closeCashbox,
            isCashboxOpen = coordinator::isCashboxOpen,
        )
    }
}
