package com.erpnext.pos.utils

import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class)
object TokenUtils {

    @OptIn(ExperimentalEncodingApi::class)
    fun decodePayload(token: String): Map<String, Any?>? {
        val parts = token.split(".")
        if (parts.size < 2) return null

        return try {
            val padToken = parts[1].padBase64()
            val decodedBytes = Base64.UrlSafe.decode(padToken)
            val payloadJson = decodedBytes.decodeToString()
            val json = Json.parseToJsonElement(payloadJson).jsonObject

            // Mapear cualquier JsonElement a Any
            json.mapValues { (_, value) -> getJsonValue(value) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Función para convertir JsonElement a Any de forma flexible
    fun getJsonValue(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.intOrNull != null -> element.int
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }

            is JsonArray -> element.map { getJsonValue(it) }
            is JsonObject -> element.mapValues { getJsonValue(it.value) }
        }
    }

    // Padding seguro para Base64
    fun String.padBase64(): String {
        val missingPadding = (4 - this.length % 4) % 4
        return this + "=".repeat(missingPadding)
    }

    // Revisar si el token expiró
    fun isExpired(token: String?): Boolean {
        if (token == null) return true
        val claims = decodePayload(token) ?: return true

        val exp = claims["exp"]?.toString()?.toLongOrNull() ?: return true
        val now = Clock.System.now().epochSeconds

        return now >= exp
    }

    fun isValid(token: String?): Boolean = token != null && !isExpired(token)
}
