package com.erpnext.pos.utils

import com.erpnext.pos.utils.view.DateTimeProvider

object BootstrapFromDatePolicy {
  const val DEFAULT_DAYS_BACK: Int = 90

  fun resolve(
      today: String = DateTimeProvider.todayDate(),
      daysBack: Int = DEFAULT_DAYS_BACK,
  ): String {
    val normalizedDaysBack = daysBack.takeIf { it > 0 } ?: DEFAULT_DAYS_BACK
    return DateTimeProvider.addDays(today, -normalizedDaysBack)
  }
}
