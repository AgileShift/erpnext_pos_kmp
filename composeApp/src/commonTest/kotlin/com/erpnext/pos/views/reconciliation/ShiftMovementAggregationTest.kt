package com.erpnext.pos.views.reconciliation

import com.erpnext.pos.localSource.preferences.ShiftMovementRecord
import com.erpnext.pos.localSource.preferences.ShiftMovementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ShiftMovementAggregationTest {

  @Test
  fun summarizesExpenseRefundAndCashOutByCurrency() = runBlocking {
    val movements =
        listOf(
            ShiftMovementRecord(
                id = "1",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.EXPENSE,
                modeOfPayment = "Efectivo CORDOBAS",
                amount = 100.0,
                currency = "NIO",
                createdAt = 1L,
            ),
            ShiftMovementRecord(
                id = "2",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.REFUND,
                modeOfPayment = "Efectivo DOLARES",
                amount = 10.0,
                currency = "USD",
                createdAt = 2L,
            ),
            ShiftMovementRecord(
                id = "3",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.CASH_OUT,
                modeOfPayment = "Efectivo CORDOBAS",
                amount = 50.0,
                currency = "NIO",
                createdAt = 3L,
            ),
            ShiftMovementRecord(
                id = "4",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.INTERNAL_TRANSFER_OUT,
                modeOfPayment = "Efectivo CORDOBAS",
                amount = 25.0,
                currency = "NIO",
                createdAt = 4L,
            ),
        )

    val summary =
        summarizeShiftMovements(
            movements = movements,
            posCurrency = "NIO",
            rateResolver = { from, to -> if (from == "USD" && to == "NIO") 36.5 else 1.0 },
        )

    assertEquals(150.0, summary.expensesByCurrency["NIO"])
    assertEquals(10.0, summary.expensesByCurrency["USD"])
    assertEquals(515.0, summary.expensesTotalInPosCurrency)
  }

  @Test
  fun ignoresNonExpenseLikeMovements() = runBlocking {
    val movements =
        listOf(
            ShiftMovementRecord(
                id = "1",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.CASH_IN,
                modeOfPayment = "Efectivo CORDOBAS",
                amount = 20.0,
                currency = "NIO",
                createdAt = 1L,
            ),
            ShiftMovementRecord(
                id = "2",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.INTERNAL_TRANSFER_IN,
                modeOfPayment = "Efectivo DOLARES",
                amount = 5.0,
                currency = "USD",
                createdAt = 2L,
            ),
            ShiftMovementRecord(
                id = "3",
                posOpeningEntry = "POE-1",
                profileName = "POS Central",
                movementType = ShiftMovementType.COLLECTION,
                modeOfPayment = "Tarjeta",
                amount = 200.0,
                currency = "NIO",
                createdAt = 3L,
            ),
        )

    val summary =
        summarizeShiftMovements(
            movements = movements,
            posCurrency = "NIO",
            rateResolver = { _, _ -> 1.0 },
        )

    assertEquals(emptyMap(), summary.expensesByCurrency)
    assertEquals(0.0, summary.expensesTotalInPosCurrency)
  }
}
