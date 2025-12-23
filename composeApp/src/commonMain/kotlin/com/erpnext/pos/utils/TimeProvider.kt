package com.erpnext.pos.utils

expect class TimeProvider() {
    fun nowMillis(): Long
}