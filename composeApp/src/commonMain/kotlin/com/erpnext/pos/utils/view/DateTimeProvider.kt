package com.erpnext.pos.utils.view

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object DateTimeProvider {
    fun todayDate(): String {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date.toString()
    }

    fun currentTime(): String {
        val now = Clock.System.now()
        val time = now.toLocalDateTime(TimeZone.currentSystemDefault()).time
        return time.toString()
    }
}