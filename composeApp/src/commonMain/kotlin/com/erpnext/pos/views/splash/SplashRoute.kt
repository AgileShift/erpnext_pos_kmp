package com.erpnext.pos.views.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun SplashRoute(
    coordinator: SplashCoordinator = rememberSplashCoordinator(),
) {
    val uiState by coordinator.screenStateFlow.collectAsState(SplashState.Loading)
    val actions = rememberSplashActions(coordinator)

    SplashScreen(uiState, actions)
}

@Composable
fun rememberSplashActions(coordinator: SplashCoordinator): SplashActions {
    return remember(coordinator) {
        SplashActions(
            isAuth = coordinator::isLoggedIn,
            initializeContext = coordinator::initializeContext,
        )
    }
}