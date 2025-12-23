package com.erpnext.pos.views.customer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class CustomerCoordinator(
    val viewModel: CustomerViewModel
) {
    val screenStateFlow = viewModel.stateFlow

    fun fetchAll() = viewModel.fetchAllCustomers()

    fun toDetails(customerId: String) = viewModel.toDetails(customerId)

    fun onSearchQueryChanged(input: String) = viewModel.onSearchQueryChanged(input)

    fun onTerritorySelected(territory: String?) = viewModel.onStateSelected(territory)

    fun checkCredit(customerId: String, amount: Double, onResult: (Boolean, String) -> Unit) =
        viewModel.checkCredit(customerId, amount, onResult)

    fun onRefresh() = viewModel.onRefresh()
}

@Composable
fun rememberCustomerCoordinator(): CustomerCoordinator {
    val viewModel: CustomerViewModel = koinViewModel()

    return remember(viewModel) {
        CustomerCoordinator(
            viewModel = viewModel
        )
    }
}