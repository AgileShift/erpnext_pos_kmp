package com.erpnext.pos.views.billing

data class BillingTotals(
    val subtotal: Double,
    val taxes: Double,
    val discount: Double,
    val shipping: Double,
    val total: Double
)

enum class DiscountSource {
    None,
    Manual,
    Code
}

data class DiscountInfo(
    val amount: Double,
    val percent: Double?,
    val source: DiscountSource
)

object BillingCalculationHelper {
    fun calculateTotals(state: BillingState.Success): BillingTotals {
        val subtotal = state.cartItems.sumOf { it.price * it.quantity }
        val taxes = 0.0
        val discountInfo = resolveDiscountInfo(state, subtotal)
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

    fun resolveDiscountInfo(state: BillingState.Success, subtotal: Double): DiscountInfo {
        val hasPercent = state.manualDiscountPercent > 0.0
        val hasAmount = state.manualDiscountAmount > 0.0
        val source = when {
            state.discountCode.isNotBlank() -> DiscountSource.Code
            hasPercent || hasAmount -> DiscountSource.Manual
            else -> DiscountSource.None
        }
        val percent = state.manualDiscountPercent.takeIf { it > 0.0 }?.coerceAtMost(100.0)
        val rawAmount = when {
            percent != null -> subtotal * (percent / 100.0)
            hasAmount -> state.manualDiscountAmount
            else -> 0.0
        }.coerceIn(0.0, subtotal)

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
}
