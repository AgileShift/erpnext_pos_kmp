package com.erpnext.pos.views.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.views.salesflow.SalesFlowSource
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
    fun onAddPaymentLine(line: PaymentLine) = viewModel.onAddPaymentLine(line)
    fun onRemovePaymentLine(index: Int) = viewModel.onRemovePaymentLine(index)
    fun onFinalizeSale() = viewModel.onFinalizeSale()
    fun onCreditSaleChanged(isCreditSale: Boolean) = viewModel.onCreditSaleChanged(isCreditSale)
    fun onPaymentTermSelected(term: com.erpnext.pos.domain.models.PaymentTermBO?) =
        viewModel.onPaymentTermSelected(term)
    fun onDiscountCodeChanged(code: String) = viewModel.onDiscountCodeChanged(code)
    fun onManualDiscountAmountChanged(value: String) = viewModel.onManualDiscountAmountChanged(value)
    fun onManualDiscountPercentChanged(value: String) = viewModel.onManualDiscountPercentChanged(value)
    fun onDeliveryChargeSelected(charge: com.erpnext.pos.domain.models.DeliveryChargeBO?) =
        viewModel.onDeliveryChargeSelected(charge)
    fun onPaymentCurrencySelected(currency: String) = viewModel.onPaymentCurrencySelected(currency)
    fun onClearSuccessMessage() = viewModel.onClearSuccessMessage()
    fun onBack() = viewModel.onBack()
    fun onLinkSource(sourceType: SalesFlowSource, sourceId: String) =
        viewModel.linkSourceDocument(sourceType, sourceId)
    fun onClearSource() = viewModel.clearSourceDocument()
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
