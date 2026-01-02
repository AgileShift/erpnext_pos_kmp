package com.erpnext.pos.views.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

class HomeCoordinator(
    val viewModel: HomeViewModel
) {
    val screenStateFlow = viewModel.stateFlow
    val syncState = viewModel.syncState
    val syncSettings = viewModel.syncSettings

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

    fun openCashbox(entry: POSProfileSimpleBO, amounts: List<PaymentModeWithAmount>) {
        viewModel.openCashbox(entry, amounts)
    }

    fun closeCashbox() {
        viewModel.closeCashbox()
    }

    fun isCashboxOpen(): StateFlow<Boolean> {
        return viewModel.isCashboxOpen()
    }

    fun onAutoSyncChanged(enabled: Boolean) {
        viewModel.setAutoSync(enabled)
    }

    fun onSyncOnStartupChanged(enabled: Boolean) {
        viewModel.setSyncOnStartup(enabled)
    }

    fun onWifiOnlyChanged(enabled: Boolean) {
        viewModel.setWifiOnly(enabled)
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
