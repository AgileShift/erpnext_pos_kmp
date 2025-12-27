package com.erpnext.pos.views.salesorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class SalesOrderCoordinator(
    private val viewModel: SalesOrderViewModel
) {
    val screenStateFlow = viewModel.stateFlow

    fun onRefresh() = viewModel.onRefresh()
    fun onBack() = viewModel.onBack()
}

@Composable
fun rememberSalesOrderCoordinator(): SalesOrderCoordinator {
    val viewModel: SalesOrderViewModel = koinViewModel()

    return remember(viewModel) {
        SalesOrderCoordinator(viewModel)
    }
}
