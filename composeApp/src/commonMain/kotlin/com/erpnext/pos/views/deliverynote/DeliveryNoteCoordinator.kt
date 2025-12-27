package com.erpnext.pos.views.deliverynote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class DeliveryNoteCoordinator(
    private val viewModel: DeliveryNoteViewModel
) {
    val screenStateFlow = viewModel.stateFlow

    fun onRefresh() = viewModel.onRefresh()
}

@Composable
fun rememberDeliveryNoteCoordinator(): DeliveryNoteCoordinator {
    val viewModel: DeliveryNoteViewModel = koinViewModel()

    return remember(viewModel) {
        DeliveryNoteCoordinator(viewModel)
    }
}
