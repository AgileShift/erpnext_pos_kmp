package com.erpnext.pos.remoteSource.sdk

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val htmlTagRegex = Regex("<[^>]*>")
private val whitespaceRegex = Regex("\\s+")

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

    val sanitized = fallbackMessage?.let { sanitizeHtml(it) }
    return sanitized?.takeIf { it.isNotBlank() } ?: defaultMessage
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
