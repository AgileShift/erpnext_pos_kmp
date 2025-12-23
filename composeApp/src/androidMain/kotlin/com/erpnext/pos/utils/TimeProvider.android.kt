package com.erpnext.pos.utils

actual class TimeProvider actual constructor() {
    actual fun nowMillis(): Long = System.currentTimeMillis()
}