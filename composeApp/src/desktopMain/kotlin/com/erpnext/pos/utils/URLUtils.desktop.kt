package com.erpnext.pos.utils

import java.net.URI

actual fun isValidUrl(url: String): Boolean {
    return try {
        val uri = URI(url)
        uri.scheme?.startsWith("http") == true && !uri.host.isNullOrEmpty()
    } catch (e: Exception) {
        false
    }
}