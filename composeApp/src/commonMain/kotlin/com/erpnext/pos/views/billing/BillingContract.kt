package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.PaymentTermBO

data class PaymentLine(
    val modeOfPayment: String,
    val enteredAmount: Double,
    val currency: String,
    val exchangeRate: Double,
    val baseAmount: Double,
    val referenceNumber: String? = null
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
        val discountCode: String = "",
        val manualDiscountAmount: Double = 0.0,
        val manualDiscountPercent: Double = 0.0,
        val shippingAmount: Double = 0.0,
        val total: Double = 0.0,
        val isCreditSale: Boolean = false,
        val paymentTerms: List<PaymentTermBO> = emptyList(),
        val selectedPaymentTerm: PaymentTermBO? = null,
        val paymentLines: List<PaymentLine> = emptyList(),
        val paymentModes: List<POSPaymentModeOption> = emptyList(),
        val allowedCurrencies: List<POSCurrencyOption> = emptyList(),
        val exchangeRateByCurrency: Map<String, Double> = emptyMap(),
        val paidAmountBase: Double = 0.0,
        val balanceDueBase: Double = 0.0,
        val changeDueBase: Double = 0.0,
        val paymentErrorMessage: String? = null,
        val successMessage: String? = null
    ) : BillingState {
        fun recalculateCartTotals(): Success {
            val totals = BillingCalculationHelper.calculateTotals(this)
            return copy(
                subtotal = totals.subtotal,
                taxes = totals.taxes,
                discount = totals.discount,
                total = totals.total
            ).recalculatePaymentTotals()
        }

        fun recalculatePaymentTotals(): Success {
            val newPaidAmountBase = paymentLines.sumOf { it.baseAmount }
            val newBalanceDueBase = (total - newPaidAmountBase).coerceAtLeast(0.0)
            val newChangeDueBase = (newPaidAmountBase - total).coerceAtLeast(0.0)
            return copy(
                paidAmountBase = newPaidAmountBase,
                balanceDueBase = newBalanceDueBase,
                changeDueBase = newChangeDueBase
            )
        }

        fun withPaymentLines(lines: List<PaymentLine>): Success {
            return copy(paymentLines = lines, paymentErrorMessage = null).recalculatePaymentTotals()
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
    val onAddPaymentLine: (PaymentLine) -> Unit = {},
    val onRemovePaymentLine: (index: Int) -> Unit = {},
    val onFinalizeSale: () -> Unit = {},
    val onCreditSaleChanged: (Boolean) -> Unit = {},
    val onPaymentTermSelected: (PaymentTermBO?) -> Unit = {},
    val onDiscountCodeChanged: (String) -> Unit = {},
    val onManualDiscountAmountChanged: (String) -> Unit = {},
    val onManualDiscountPercentChanged: (String) -> Unit = {},
    val onShippingAmountChanged: (String) -> Unit = {},
    val onPaymentCurrencySelected: (String) -> Unit = {},
    val onClearSuccessMessage: () -> Unit = {},
    val onBack: () -> Unit = {},
)
