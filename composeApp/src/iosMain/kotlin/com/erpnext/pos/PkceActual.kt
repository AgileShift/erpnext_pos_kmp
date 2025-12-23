package com.erpnext.pos

import platform.Foundation.NSData
import platform.Security.*
import kotlinx.cinterop.*
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create

actual fun sha256(bytes: ByteArray): ByteArray {
    return bytes.usePinned { pinned ->
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        CC_SHA256(pinned.addressOf(0), bytes.size.convert(), digest.refTo(0))
        digest.toByteArray()
    }
}

actual fun base64UrlNoPad(bytes: ByteArray): String {
    val data = bytes.toNSData()
    val base64 = data.base64EncodedStringWithOptions(0)
    return base64.replace('+', '-').replace('/', '_').trimEnd('=')
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    NSData.create(bytes = this.toCValues(), length = size.toULong())

actual fun randomUrlSafe(len: Int): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    return (0 until len).map { alphabet.random() }.joinToString("")
}