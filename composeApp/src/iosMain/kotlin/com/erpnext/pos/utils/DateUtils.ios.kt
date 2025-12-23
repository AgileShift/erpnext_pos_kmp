package com.erpnext.pos.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatMonthName(date: LocalDate): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "LLLL"
        locale = NSLocale.currentLocale
    }

    val components = date.toNSDateComponents()
    val calendar = NSCalendar.currentCalendar
    val nsDate = calendar.dateFromComponents(components)!!

    return formatter.stringFromDate(nsDate)
}

actual fun formatWeekdayName(date: LocalDate): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "EEEE"
        locale = NSLocale.currentLocale
    }

    val components = date.toNSDateComponents()
    val calendar = NSCalendar.currentCalendar
    val nsDate = calendar.dateFromComponents(components)!!

    return formatter.stringFromDate(nsDate)
}