package com.erpnext.pos

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

actual fun sha256(bytes: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(bytes)

actual fun base64UrlNoPad(bytes: ByteArray): String =
    Base64.encodeToString(bytes, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)

actual fun randomUrlSafe(len: Int): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val rnd = SecureRandom()
    return (1..len).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
}