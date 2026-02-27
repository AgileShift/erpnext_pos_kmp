package com.erpnext.pos.utils

import android.util.Log

actual object AppLogger {
    private const val TAG = "ERPNextPOS"

    actual fun info(message: String) {
        Log.i(TAG, message)
        AppSentry.breadcrumb(message)
    }

    actual fun warn(message: String, throwable: Throwable?, reportToSentry: Boolean) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
        if (throwable != null && reportToSentry) {
            AppSentry.capture(throwable, message)
        } else {
            AppSentry.breadcrumb(message)
        }
    }
}
