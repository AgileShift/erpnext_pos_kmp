package com.erpnext.pos.domain.usecases

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReturnStockImpactResolverTest {

  @Test
  fun explicitTrue_alwaysAffectsInventory() {
    val resolved = resolveReturnStockImpact(isPosInvoice = false, explicitSelection = true)
    assertTrue(resolved)
  }

  @Test
  fun explicitFalse_neverAffectsInventory() {
    val resolved = resolveReturnStockImpact(isPosInvoice = true, explicitSelection = false)
    assertFalse(resolved)
  }

  @Test
  fun nullSelection_posInvoiceDefaultsToAffectInventory() {
    val resolved = resolveReturnStockImpact(isPosInvoice = true, explicitSelection = null)
    assertTrue(resolved)
  }

  @Test
  fun nullSelection_nonPosInvoiceDefaultsToAccountingOnly() {
    val resolved = resolveReturnStockImpact(isPosInvoice = false, explicitSelection = null)
    assertFalse(resolved)
  }
}
