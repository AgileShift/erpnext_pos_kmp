package com.erpnext.pos.views.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.koinInject

/**
 * Screen's coordinator which is responsible for handling actions from the UI layer
 * and one-shot actions based on the new UI state
 */
class SplashCoordinator(
    val viewModel: SplashViewModel,
) {
    val screenStateFlow = viewModel.stateFlow

    fun isLoggedIn() = viewModel.verifyToken()

    suspend fun initializeContext() = viewModel.initializeContext()
}

@Composable
fun rememberSplashCoordinator(): SplashCoordinator {
    val viewModel: SplashViewModel = koinInject()

    return remember(viewModel) {
        SplashCoordinator(
            viewModel = viewModel,
        )
    }
}