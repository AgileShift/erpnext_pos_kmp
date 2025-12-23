package com.erpnext.pos.utils

expect fun isValidUrl(url: String): Boolean

fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

// Validación básica: no encadena, solo comprueba que tenga un dominio válido
fun isValidUrlInput(input: String): Boolean {
    val regex = Regex("^(https?://)?([a-zA-Z0-9.-]+)\\.[a-zA-Z]{2,}.*$")
    return regex.matches(input.trim())
}