package com.erpnext.pos.views.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

class HomeCoordinator(
    val viewModel: HomeViewModel
) {
    val screenStateFlow = viewModel.stateFlow
    val syncState = viewModel.syncState
    val syncSettings = viewModel.syncSettings
    val homeMetrics = viewModel.homeMetrics

    fun loadInitialData() {
        return viewModel.loadInitialData()
    }

    fun sync() {
        viewModel.syncNow()
    }

    fun initialState() {
        viewModel.resetToInitialState()
    }

    fun logout() {
        return viewModel.logout()
    }

    fun onError(error: String) {
        viewModel.onError(error)
    }

    fun onPosSelected(pos: POSProfileSimpleBO) {
        viewModel.onPosSelected(pos)
    }

    suspend fun openCashbox(entry: POSProfileSimpleBO, amounts: List<PaymentModeWithAmount>) {
        viewModel.openCashbox(entry, amounts)
    }

    fun closeCashbox() {
        viewModel.closeCashbox()
    }

    fun isCashboxOpen(): StateFlow<Boolean> {
        return viewModel.isCashboxOpen()
    }

    fun openSettings() {
        viewModel.openSettings()
    }

    fun openReconciliation() {
        viewModel.openReconciliation()
    }

    fun openCloseCashbox() {
        viewModel.openCloseCashbox()
    }
}

@Composable
fun rememberHomeCoordinator(): HomeCoordinator {
    val viewModel: HomeViewModel = koinInject()

    return remember(viewModel) {
        HomeCoordinator(
            viewModel = viewModel
        )
    }
}
