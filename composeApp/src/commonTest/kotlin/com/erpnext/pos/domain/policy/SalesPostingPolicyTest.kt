package com.erpnext.pos.domain.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SalesPostingPolicyTest {

  @Test
  fun cashSale_isAlwaysAllowed_asPosPaid() {
    val result =
        SalesPostingPolicy.decide(
            totalAmount = 100.0,
            paidAmount = 100.0,
            isCreditSale = false,
            allowPartialPayment = false,
            tolerance = 0.01,
        )

    assertTrue(result is SalesPostingResolution.Allowed)
    val decision = result.decision
    assertEquals(SalesPostingType.PosCash, decision.type)
    assertTrue(decision.isPos)
    assertEquals("Paid", decision.status)
    assertEquals(100.0, decision.paidAmount)
    assertEquals(0.0, decision.outstandingAmount)
  }

  @Test
  fun partialCreditSale_allowedWhenProfileEnablesPartialPayment() {
    val result =
        SalesPostingPolicy.decide(
            totalAmount = 150.0,
            paidAmount = 60.0,
            isCreditSale = true,
            allowPartialPayment = true,
            tolerance = 0.01,
        )

    assertTrue(result is SalesPostingResolution.Allowed)
    val decision = result.decision
    assertEquals(SalesPostingType.PosPartlyPaid, decision.type)
    assertTrue(decision.isPos)
    assertEquals("Partly Paid", decision.status)
    assertEquals(60.0, decision.paidAmount)
    assertEquals(90.0, decision.outstandingAmount)
  }

  @Test
  fun partialCreditSale_isBlockedWhenProfileDisallowsPartialPayment() {
    val result =
        SalesPostingPolicy.decide(
            totalAmount = 150.0,
            paidAmount = 60.0,
            isCreditSale = true,
            allowPartialPayment = false,
            tolerance = 0.01,
        )

    assertTrue(result is SalesPostingResolution.Blocked)
    val blocked = result
    assertEquals(SalesPostingBlockReason.PartialPaymentNotAllowedByProfile, blocked.reason)
  }

  @Test
  fun fullCreditSale_routesToNonPos_andStaysOutOfPosReconciliation() {
    val result =
        SalesPostingPolicy.decide(
            totalAmount = 220.0,
            paidAmount = 0.0,
            isCreditSale = true,
            allowPartialPayment = true,
            tolerance = 0.01,
        )

    assertTrue(result is SalesPostingResolution.Allowed)
    val decision = result.decision
    assertEquals(SalesPostingType.NonPosCredit, decision.type)
    assertFalse(decision.isPos)
    assertEquals("Unpaid", decision.status)
    assertEquals(0.0, decision.paidAmount)
    assertEquals(220.0, decision.outstandingAmount)
  }
}
