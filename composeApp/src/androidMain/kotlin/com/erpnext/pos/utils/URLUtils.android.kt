package com.erpnext.pos.utils

import androidx.core.net.toUri

actual fun isValidUrl(url: String): Boolean {
    return try {
        val uri = url.toUri()
        uri.scheme?.startsWith("http") == true && !uri.host.isNullOrEmpty()
    } catch (e: Exception) {
        false
    }
}