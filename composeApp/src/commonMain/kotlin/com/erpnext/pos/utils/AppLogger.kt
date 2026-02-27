package com.erpnext.pos.utils

expect object AppLogger {
    fun info(message: String)
    fun warn(message: String, throwable: Throwable? = null, reportToSentry: Boolean = true)
}
