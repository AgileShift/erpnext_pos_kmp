package com.erpnext.pos.views.login

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRoute(
    coordinator: LoginCoordinator = rememberLoginCoordinator(),
) {
    val uiState by coordinator.screenStateFlow.collectAsState(LoginState.Loading)
    val actions = rememberLoginActions(coordinator)

    LoginScreen(uiState, actions)
}

@Composable
fun rememberLoginActions(coordinator: LoginCoordinator): LoginAction {
    return remember(coordinator) {
        LoginAction(
            existingSites = coordinator::existingSites,
            onSiteSelected = coordinator::onSiteSelected,
            onAddSite = coordinator::onAddSite,
            onError = coordinator::onError,
            onReset = coordinator::onReset,
            isAuthenticated = coordinator::isAuthenticated
        )
    }
}