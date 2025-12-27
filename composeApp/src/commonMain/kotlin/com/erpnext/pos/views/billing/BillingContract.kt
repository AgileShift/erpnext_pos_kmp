package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO

/**
 * Data classes for state and actions, assuming a more robust structure.
 * You should move these to your state and action files accordingly.
 */
sealed interface BillingState {
    object Loading : BillingState
    data class Success(
        // Customer-related state
        val customers: List<CustomerBO> = emptyList(),
        val selectedCustomer: CustomerBO? = null,
        val customerSearchQuery: String = "",

        // Product-related state
        val productSearchQuery: String = "",
        val productSearchResults: List<ItemBO> = emptyList(),

        // Cart-related state
        val currency: String?,
        val exchangeRate: Double?,
        val cartItems: List<CartItem> = emptyList(),
        val subtotal: Double = 0.0,
        val taxes: Double = 0.0,
        val discount: Double = 0.0,
        val total: Double = 0.0
    ) : BillingState

    data class Error(val message: String) : BillingState
    object Empty : BillingState
}

data class BillingAction(
    val onCustomerSearchQueryChange: (String) -> Unit = {},
    val onCustomerSelected: (CustomerBO) -> Unit = {},
    val onProductSearchQueryChange: (String) -> Unit = {},
    val onProductAdded: (ItemBO) -> Unit = {},
    val onQuantityChanged: (itemCode: String, newQuantity: Double) -> Unit = { _, _ -> },
    val onRemoveItem: (itemCode: String) -> Unit = {},
    val onFinalizeSale: () -> Unit = {},
    val onBack: () -> Unit = {},
)