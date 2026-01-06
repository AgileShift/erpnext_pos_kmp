package com.erpnext.pos.utils

actual object AppLogger {
    actual fun info(message: String) {
        // No-op for iOS for now.
    }

    actual fun warn(message: String, throwable: Throwable?) {
        // No-op for iOS for now.
    }
}
