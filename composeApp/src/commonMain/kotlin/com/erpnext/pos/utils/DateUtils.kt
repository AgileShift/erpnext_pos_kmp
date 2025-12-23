package com.erpnext.pos.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
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