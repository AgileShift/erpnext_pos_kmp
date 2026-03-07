package com.erpnext.pos

import com.erpnext.pos.utils.BootstrapFromDatePolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class BootstrapFromDatePolicyTest {

  @Test
  fun resolve_uses_ninety_days_by_default() {
    val fromDate = BootstrapFromDatePolicy.resolve(today = "2026-03-05")

    assertEquals("2025-12-05", fromDate)
  }

  @Test
  fun resolve_uses_custom_positive_days_back() {
    val fromDate = BootstrapFromDatePolicy.resolve(today = "2026-03-05", daysBack = 30)

    assertEquals("2026-02-03", fromDate)
  }

  @Test
  fun resolve_falls_back_to_default_when_days_back_is_not_positive() {
    val fromDate = BootstrapFromDatePolicy.resolve(today = "2026-03-05", daysBack = 0)

    assertEquals("2025-12-05", fromDate)
  }
}
