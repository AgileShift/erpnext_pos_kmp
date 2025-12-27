package com.erpnext.pos.views.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class BillingCoordinator(val viewModel: BillingViewModel) {

    val screenStateFlow = viewModel.state

    fun onCustomerSearchQueryChange(query: String) = viewModel.onCustomerSearchQueryChange(query)
    fun onCustomerSelected(customer: com.erpnext.pos.domain.models.CustomerBO) =
        viewModel.onCustomerSelected(customer)
    fun onProductSearchQueryChange(query: String) = viewModel.onProductSearchQueryChange(query)
    fun onProductAdded(item: com.erpnext.pos.domain.models.ItemBO) = viewModel.onProductAdded(item)
    fun onQuantityChanged(itemCode: String, newQuantity: Double) =
        viewModel.onQuantityChanged(itemCode, newQuantity)
    fun onRemoveItem(itemCode: String) = viewModel.onRemoveItem(itemCode)
    fun onFinalizeSale() = viewModel.onFinalizeSale()
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
