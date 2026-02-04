package com.erpnext.pos.remoteSource.sdk

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val htmlTagRegex = Regex("<[^>]*>")
private val whitespaceRegex = Regex("\\s+")
private val itemFromLinkRegex = Regex("/app/Form/Item/([^\"\\s?/]+)")
private val itemFromProductLabelRegex = Regex("Producto\\s+([A-Za-z0-9._-]+)")

fun Throwable.toUserMessage(
    defaultMessage: String = "Ocurrió un error. Inténtalo nuevamente."
): String {
    val frappeException = this as? FrappeException
    val errorResponse = frappeException?.errorResponse

    val serverMessage = errorResponse?._server_messages?.takeIf { it.isNotBlank() }
    val formattedServerMessage = serverMessage?.let { parseServerMessages(it) }
    val fallbackMessage = formattedServerMessage
        ?: errorResponse?.exception
        ?: frappeException?.message
        ?: message

    if (isReservedStockError()) {
        val itemCode = extractReservedStockItemCode()
        return if (itemCode.isNullOrBlank()) {
            "No se puede completar la venta porque el stock está reservado para otras órdenes."
        } else {
            "No se puede completar la venta: el artículo $itemCode está reservado para otras órdenes."
        }
    }

    val sanitized = fallbackMessage?.let { sanitizeHtml(it) }
    return sanitized?.takeIf { it.isNotBlank() } ?: defaultMessage
}

fun Throwable.isReservedStockError(): Boolean {
    return collectErrorTexts().any { raw ->
        val normalized = raw.lowercase()
        normalized.contains("negativestockerror") &&
            (
                normalized.contains("reserved for other sales orders") ||
                    normalized.contains("stock is reserved for other sales orders") ||
                    normalized.contains("reserved_qty")
                )
    }
}

fun Throwable.extractReservedStockItemCode(): String? {
    val candidates = collectErrorTexts()
    val byLink = candidates.firstNotNullOfOrNull { text ->
        itemFromLinkRegex.find(text)?.groupValues?.getOrNull(1)
    }
    if (!byLink.isNullOrBlank()) return byLink

    return candidates.firstNotNullOfOrNull { text ->
        val sanitized = sanitizeHtml(text)
        itemFromProductLabelRegex.find(sanitized)?.groupValues?.getOrNull(1)
    }
}

private fun Throwable.collectErrorTexts(): List<String> {
    val messages = mutableListOf<String>()
    var current: Throwable? = this
    while (current != null) {
        val frappe = current as? FrappeException
        frappe?.errorResponse?._server_messages?.let { messages += it }
        frappe?.errorResponse?.exception?.let { messages += it }
        current.message?.let { messages += it }
        current = current.cause
    }
    return messages
}

private fun parseServerMessages(raw: String): String? {
    val parsed = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return null
    if (parsed is JsonArray) {
        val firstMessage = parsed.firstOrNull() ?: return null
        val rawMessage = firstMessage.jsonPrimitive.contentOrNull ?: return null
        val payload = runCatching { json.parseToJsonElement(rawMessage) }.getOrNull()
        return payload?.extractMessage() ?: rawMessage
    }
    return parsed.extractMessage()
}

private fun JsonElement.extractMessage(): String? {
    val message = jsonObject["message"]?.jsonPrimitive?.contentOrNull
    val title = jsonObject["title"]?.jsonPrimitive?.contentOrNull
    return listOfNotNull(title, message).joinToString(": ").takeIf { it.isNotBlank() }
}

private fun sanitizeHtml(raw: String): String {
    return raw.replace(htmlTagRegex, " ")
        .replace(whitespaceRegex, " ")
        .trim()
}
