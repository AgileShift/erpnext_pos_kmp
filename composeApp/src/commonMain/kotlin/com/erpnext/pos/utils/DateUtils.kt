@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

expect fun formatMonthName(date: LocalDate): String
expect fun formatWeekdayName(date: LocalDate): String

fun LocalDateTime.toLocalDate(): LocalDate = date

@OptIn(ExperimentalTime::class)
fun datetimeNow(): String {
    val tz = TimeZone.currentSystemDefault()
    val ldt = Clock.System.now().toLocalDateTime(tz)
    val year = ldt.date.year
    val month = formatMonthName(ldt.toLocalDate())
    val day = ldt.date.day
    val weekday = formatWeekdayName(ldt.toLocalDate())

    val date = "$weekday $day de $month del $year"
    return date
}

/** Convierte epoch millis a "yyyy-MM-dd HH:mm:ss" usando la zona del sistema. */
@OptIn(ExperimentalTime::class)
fun epochMillisToErpDateTime(
    epochMillis: Long,
    tz: TimeZone = TimeZone.currentSystemDefault()
): String {
    val ldt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz)
    val year = ldt.date.year
    val month = ldt.date.month.number
    val day = ldt.date.day
    val hour = ldt.hour
    val minute = ldt.minute
    val second = ldt.second

    return "$year-$month-$day $hour:$minute:$second"
}

/** Convierte epoch millis a "yyyy-MM-dd" (fecha sin hora). */
@OptIn(ExperimentalTime::class)
fun epochMillisToErpDate(
    epochMillis: Long,
    tz: TimeZone = TimeZone.currentSystemDefault()
): String {
    val ldt = kotlin.time.Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz)
    val year = ldt.date.year
    val month = ldt.date.month.number
    val day = ldt.date.day

    return "$year-$month-$day"
}

/** Extensiones rápidas */
fun Long.toErpDateTime(tz: TimeZone = TimeZone.currentSystemDefault()) =
    epochMillisToErpDateTime(this, tz)

fun Long.toErpDate(tz: TimeZone = TimeZone.currentSystemDefault()) = epochMillisToErpDate(this, tz)

@OptIn(ExperimentalTime::class)
fun nowErpDateTime(tz: TimeZone = TimeZone.currentSystemDefault()) =
    epochMillisToErpDateTime(Clock.System.now().toEpochMilliseconds(), tz)

fun parseErpDateTimeToEpochMillis(
    value: String?,
    tz: TimeZone = TimeZone.currentSystemDefault()
): Long? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    val parts = trimmed.split(" ")
    val datePart = parts.getOrNull(0)?.trim().orEmpty()
    if (datePart.isBlank()) return null
    val timePart = parts.getOrNull(1)?.trim().orEmpty()
    val dateTokens = datePart.split("-")
    if (dateTokens.size < 3) return null
    val year = dateTokens[0].toIntOrNull() ?: return null
    val month = dateTokens[1].toIntOrNull() ?: return null
    val day = dateTokens[2].toIntOrNull() ?: return null
    val timeTokens = if (timePart.isBlank()) emptyList() else timePart.split(":")
    val hour = timeTokens.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = timeTokens.getOrNull(1)?.toIntOrNull() ?: 0
    val second = timeTokens.getOrNull(2)?.toIntOrNull() ?: 0
    return runCatching {
        LocalDateTime(year, month, day, hour, minute, second)
            .toInstant(tz)
            .toEpochMilliseconds()
    }.getOrNull()
}
