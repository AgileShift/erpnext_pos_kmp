package com.erpnext.pos.views.reconciliation

import com.erpnext.pos.localSource.preferences.ShiftMovementRecord
import com.erpnext.pos.localSource.preferences.ShiftMovementType
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.roundToCurrency

data class ShiftMovementSummary(
    val expensesByCurrency: Map<String, Double>,
    val expensesTotalInPosCurrency: Double,
)

suspend fun summarizeShiftMovements(
    movements: List<ShiftMovementRecord>,
    posCurrency: String,
    rateResolver: suspend (fromCurrency: String, toCurrency: String) -> Double?,
): ShiftMovementSummary {
  val expenseLike =
      movements.filter { movement ->
        movement.movementType == ShiftMovementType.EXPENSE ||
            movement.movementType == ShiftMovementType.REFUND ||
            movement.movementType == ShiftMovementType.CASH_OUT
      }
  if (expenseLike.isEmpty()) {
    return ShiftMovementSummary(expensesByCurrency = emptyMap(), expensesTotalInPosCurrency = 0.0)
  }

  val byCurrency = mutableMapOf<String, Double>()
  var totalPos = 0.0
  val normalizedPos = normalizeCurrency(posCurrency)

  expenseLike.forEach { movement ->
    val currency = normalizeCurrency(movement.currency)
    byCurrency[currency] = (byCurrency[currency] ?: 0.0) + movement.amount
    val converted =
        if (currency.equals(normalizedPos, ignoreCase = true)) {
          movement.amount
        } else {
          val rate = rateResolver(currency, normalizedPos) ?: 1.0
          movement.amount * rate
        }
    totalPos += converted
  }

  return ShiftMovementSummary(
      expensesByCurrency = byCurrency.mapValues { (_, amount) -> roundToCurrency(amount) },
      expensesTotalInPosCurrency = roundToCurrency(totalPos),
  )
}
