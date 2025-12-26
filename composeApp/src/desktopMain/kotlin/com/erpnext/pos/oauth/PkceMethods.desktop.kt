package com.erpnext.pos

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64

actual fun sha256(bytes: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(bytes)

//TODO: Validar este metodo con el startIndex (URL_SAFE, NO_PADDING)
actual fun base64UrlNoPad(bytes: ByteArray): String {
    return Base64.UrlSafe
        .withPadding(Base64.PaddingOption.ABSENT)
        .encode(bytes)
}

actual fun randomUrlSafe(len: Int): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val rnd = SecureRandom()
    return (1..len).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
}