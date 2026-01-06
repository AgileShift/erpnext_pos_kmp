package com.erpnext.pos.utils

import com.erpnext.pos.DesktopLogger

actual object AppLogger {
    actual fun info(message: String) {
        DesktopLogger.info(message)
        AppSentry.breadcrumb(message)
    }

    actual fun warn(message: String, throwable: Throwable?) {
        DesktopLogger.warn(message, throwable)
        if (throwable != null) {
            AppSentry.capture(throwable, message)
        } else {
            AppSentry.breadcrumb(message)
        }
    }
}
