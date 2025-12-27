package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO

data class PaymentLine(
    val modeOfPayment: String,
    val enteredAmount: Double,
    val currency: String,
    val exchangeRate: Double,
    val baseAmount: Double,
    val reference: String? = null
)

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
        val total: Double = 0.0,
        val paymentLines: List<PaymentLine> = emptyList(),
        val paidAmount: Double = 0.0,
        val balanceDue: Double = 0.0
    ) : BillingState {
        fun recalculateCartTotals(): Success {
            val newSubtotal = cartItems.sumOf { it.price * it.quantity }
            val newTaxes = 0.0
            val newDiscount = 0.0
            val newTotal = newSubtotal + newTaxes - newDiscount
            return copy(
                subtotal = newSubtotal,
                taxes = newTaxes,
                discount = newDiscount,
                total = newTotal
            ).recalculatePaymentTotals()
        }

        fun recalculatePaymentTotals(): Success {
            val newPaidAmount = paymentLines.sumOf { it.baseAmount }
            return copy(
                paidAmount = newPaidAmount,
                balanceDue = total - newPaidAmount
            )
        }

        fun withPaymentLines(lines: List<PaymentLine>): Success {
            return copy(paymentLines = lines).recalculatePaymentTotals()
        }
    }

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
