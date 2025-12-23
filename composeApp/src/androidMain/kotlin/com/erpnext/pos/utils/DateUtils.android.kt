package com.erpnext.pos.utils

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
actual fun formatMonthName(date: LocalDate): String {
    val date = java.time.LocalDateTime
        .of(
            date.year,
            date.month.ordinal + 1,
            date.day,
            0,
            0
        )

    return DateTimeFormatter
        .ofPattern("LLLL", Locale.getDefault())
        .format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@RequiresApi(Build.VERSION_CODES.O)
actual fun formatWeekdayName(date: LocalDate): String {
    val date = java.time.LocalDateTime
        .of(
            date.year,
            date.month.ordinal + 1,
            date.day,
            0,
            0
        )

    return DateTimeFormatter
        .ofPattern("EEEE", Locale.getDefault())
        .format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}