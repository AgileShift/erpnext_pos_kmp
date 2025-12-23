package com.erpnext.pos

expect fun sha256(bytes: ByteArray): ByteArray
expect fun base64UrlNoPad(bytes: ByteArray): String
expect fun randomUrlSafe(len: Int): String