package com.erpnext.pos.views.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class BillingCoordinator(val viewModel: BillingViewModel) {

    val screenStateFlow = viewModel.state

    fun fetchAllCustomers() {}

    fun fetchAllProducts() {}

    fun checkCredit() {}

    fun loadPosProfile() {}
}

@Composable
fun rememberBillingCoordinator(): BillingCoordinator {
    val viewModel: BillingViewModel = koinViewModel()

    return remember(viewModel) {
        BillingCoordinator(
            viewModel = viewModel
        )
    }
}