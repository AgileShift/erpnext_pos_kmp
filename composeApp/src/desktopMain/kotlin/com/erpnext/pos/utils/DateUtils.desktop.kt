package com.erpnext.pos.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.LocalDate

actual fun formatMonthName(date: LocalDate): String {
  val date = LocalDateTime.of(date.year, date.month.ordinal + 1, date.day, 0, 0)

  return DateTimeFormatter.ofPattern("LLLL", Locale.getDefault()).format(date).replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
  }
}

actual fun formatWeekdayName(date: LocalDate): String {
  val date = java.time.LocalDateTime.of(date.year, date.month.ordinal + 1, date.day, 0, 0)

  return DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(date).replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
  }
}
