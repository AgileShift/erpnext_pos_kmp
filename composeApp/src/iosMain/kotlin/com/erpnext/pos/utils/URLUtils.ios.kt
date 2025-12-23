package com.erpnext.pos.utils

import platform.Foundation.NSURL

actual fun isValidUrl(url: String): Boolean {
    return NSURL.URLWithString(url) != null
}