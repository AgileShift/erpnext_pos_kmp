package com.erpnext.pos.utils

import com.erpnext.pos.base64UrlNoPad
import com.erpnext.pos.sha256

fun instanceKeyFromUrl(url: String?): String {
    val normalized = url
        ?.let { normalizeUrl(it) }
        ?.trim()
        ?.lowercase()
        .orEmpty()
    if (normalized.isBlank()) return "default"
    return base64UrlNoPad(sha256(normalized.encodeToByteArray()))
}
