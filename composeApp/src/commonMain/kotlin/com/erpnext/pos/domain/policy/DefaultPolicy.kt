package com.erpnext.pos.domain.policy

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class PolicyInput(
    var monthsBack: Int = 3
)

@OptIn(ExperimentalTime::class)
class DefaultPolicy(
    private val input: PolicyInput
) : DatePolicy {
    private val clock = Clock.System
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    override fun invoicesFromDate(): String {
        val today = clock.now().toLocalDateTime(timeZone).date
        return today.minus(DatePeriod(months = input.monthsBack)).toString()
    }
}