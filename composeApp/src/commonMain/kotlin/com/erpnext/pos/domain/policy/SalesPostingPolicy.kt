package com.erpnext.pos.domain.policy

import com.erpnext.pos.utils.roundToCurrency

enum class SalesPostingType {
    PosCash,
    PosPartlyPaid,
    NonPosCredit,
}

enum class SalesPostingBlockReason {
    CashSaleRequiresFullPayment,
    CreditSaleCannotBeFullyPaid,
    PartialPaymentNotAllowedByProfile,
}

sealed interface SalesPostingResolution {
    data class Allowed(val decision: SalesPostingDecision) : SalesPostingResolution

    data class Blocked(val reason: SalesPostingBlockReason) : SalesPostingResolution
}

data class SalesPostingDecision(
    val type: SalesPostingType,
    val paidAmount: Double,
    val outstandingAmount: Double,
    val changeAmount: Double,
    val status: String,
) {
    val isPos: Boolean
        get() = type != SalesPostingType.NonPosCredit
}

object SalesPostingPolicy {
    fun decide(
        totalAmount: Double,
        paidAmount: Double,
        isCreditSale: Boolean,
        allowPartialPayment: Boolean,
        tolerance: Double,
    ): SalesPostingResolution {
        val total = roundToCurrency(totalAmount.coerceAtLeast(0.0))
        val paidRaw = roundToCurrency(paidAmount.coerceAtLeast(0.0))
        val resolvedTolerance = tolerance.coerceAtLeast(0.0)

        if (!isCreditSale) {
            if (paidRaw + resolvedTolerance < total) {
                return SalesPostingResolution.Blocked(SalesPostingBlockReason.CashSaleRequiresFullPayment)
            }
            val change = roundToCurrency((paidRaw - total).coerceAtLeast(0.0))
            return SalesPostingResolution.Allowed(
                SalesPostingDecision(
                    type = SalesPostingType.PosCash,
                    paidAmount = total,
                    outstandingAmount = 0.0,
                    changeAmount = change,
                    status = "Paid",
                )
            )
        }

        if (paidRaw > resolvedTolerance && paidRaw + resolvedTolerance >= total) {
            return SalesPostingResolution.Blocked(SalesPostingBlockReason.CreditSaleCannotBeFullyPaid)
        }

        if (paidRaw <= resolvedTolerance) {
            return SalesPostingResolution.Allowed(
                SalesPostingDecision(
                    type = SalesPostingType.NonPosCredit,
                    paidAmount = 0.0,
                    outstandingAmount = total,
                    changeAmount = 0.0,
                    status = "Unpaid",
                )
            )
        }

        if (!allowPartialPayment) {
            return SalesPostingResolution.Blocked(
                SalesPostingBlockReason.PartialPaymentNotAllowedByProfile
            )
        }

        val paidApplied = paidRaw.coerceAtMost(total)
        val outstanding = roundToCurrency((total - paidApplied).coerceAtLeast(0.0))
        return SalesPostingResolution.Allowed(
            SalesPostingDecision(
                type = SalesPostingType.PosPartlyPaid,
                paidAmount = paidApplied,
                outstandingAmount = outstanding,
                changeAmount = 0.0,
                status = if (outstanding <= resolvedTolerance) "Paid" else "Partly Paid",
            )
        )
    }
}
