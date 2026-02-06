package com.erpnext.pos.views.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

class HomeCoordinator(
    val viewModel: HomeViewModel
) {
    val screenStateFlow = viewModel.stateFlow
    val syncState = viewModel.syncState
    val syncSettings = viewModel.syncSettings
    val homeMetrics = viewModel.homeMetrics
    val openingState = viewModel.openingState
    val openingEntryId = viewModel.openingEntryId
    val inventoryAlertMessage = viewModel.inventoryAlertMessage

    fun loadInitialData() {
        return viewModel.loadInitialData()
    }

    fun sync() {
        viewModel.syncNow()
    }

    fun cancelSync() {
        viewModel.cancelSync()
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

    fun loadOpeningProfile(profileId: String?) {
        viewModel.loadOpeningProfile(profileId)
    }

    fun openCashbox(entry: POSProfileSimpleBO, amounts: List<com.erpnext.pos.views.PaymentModeWithAmount>) {
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

    fun consumeInventoryAlertMessage() {
        viewModel.consumeInventoryAlertMessage()
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
