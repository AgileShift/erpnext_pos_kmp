package com.erpnext.pos.utils

import com.erpnext.pos.domain.models.BillingTotals
import com.erpnext.pos.domain.models.DiscountInfo
import com.erpnext.pos.domain.models.DiscountSource
import com.erpnext.pos.views.billing.BillingState

/**
 * Utilidades puras de Billing (totales / descuentos).
 * Mantener aquí para evitar acoplar PaymentUtils a BillingState.
 */
fun calculateTotals(state: BillingState.Success): BillingTotals {
    val subtotal = state.cartItems.sumOf { it.price * it.quantity }
    val taxes = state.sourceDocument?.totals?.taxTotal ?: 0.0
    val discountInfo = resolveDiscountInfo(state, subtotal, taxes)
    val shippingAmount = state.shippingAmount.coerceAtLeast(0.0)
    val total = (subtotal + taxes - discountInfo.amount + shippingAmount).coerceAtLeast(0.0)
    return BillingTotals(
        subtotal = subtotal,
        taxes = taxes,
        discount = discountInfo.amount,
        shipping = shippingAmount,
        total = total
    )
}

fun resolveDiscountInfo(state: BillingState.Success, subtotal: Double, taxes: Double = 0.0): DiscountInfo {
    val hasPercent = state.manualDiscountPercent > 0.0
    val hasAmount = state.manualDiscountAmount > 0.0
    val source = when {
        state.discountCode.isNotBlank() -> DiscountSource.Code
        hasPercent || hasAmount -> DiscountSource.Manual
        else -> DiscountSource.None
    }
    val percent = state.manualDiscountPercent.takeIf { it > 0.0 }?.coerceAtMost(100.0)
    val discountBase = if (state.applyDiscountOn.equals("Grand Total", ignoreCase = true)) {
        subtotal + taxes
    } else {
        subtotal
    }
    val rawAmount = when {
        percent != null -> discountBase * (percent / 100.0)
        hasAmount -> state.manualDiscountAmount
        else -> 0.0
    }.coerceIn(0.0, discountBase)

    val effectiveAmount = when (source) {
        DiscountSource.None -> 0.0
        DiscountSource.Manual -> rawAmount
        DiscountSource.Code -> rawAmount
    }
    return DiscountInfo(
        amount = effectiveAmount,
        percent = percent.takeIf { effectiveAmount > 0.0 },
        source = source
    )
}
