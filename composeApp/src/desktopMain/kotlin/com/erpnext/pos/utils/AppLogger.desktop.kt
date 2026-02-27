package com.erpnext.pos.utils

import com.erpnext.pos.DesktopLogger
import kotlinx.coroutines.CancellationException

actual object AppLogger {
    actual fun info(message: String) {
        DesktopLogger.info(message)
        AppSentry.breadcrumb(message)
    }

    actual fun warn(message: String, throwable: Throwable?, reportToSentry: Boolean) {
        DesktopLogger.warn(message, throwable)
        if (throwable is CancellationException) {
            AppSentry.breadcrumb("$message (cancelled)")
        } else if (throwable != null && reportToSentry) {
            AppSentry.capture(throwable, message)
        } else {
            AppSentry.breadcrumb(message)
        }
    }
}
