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
    val trimmed = input.trim()
    if (trimmed.isBlank()) return false
    val normalized = normalizeUrl(trimmed)
    return isValidUrl(normalized)
}
