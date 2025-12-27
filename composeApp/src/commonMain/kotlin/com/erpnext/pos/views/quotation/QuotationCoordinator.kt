package com.erpnext.pos.views.quotation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class QuotationCoordinator(
    private val viewModel: QuotationViewModel
) {
    val screenStateFlow = viewModel.stateFlow

    fun onRefresh() = viewModel.onRefresh()
}

@Composable
fun rememberQuotationCoordinator(): QuotationCoordinator {
    val viewModel: QuotationViewModel = koinViewModel()

    return remember(viewModel) {
        QuotationCoordinator(viewModel)
    }
}
