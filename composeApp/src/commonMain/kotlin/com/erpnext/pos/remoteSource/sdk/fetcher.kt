package com.erpnext.pos.remoteSource.sdk

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

// -----------------------------
// Configuración JSON global
// -----------------------------
val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// -----------------------------
// Funciones HTTP genéricas
// -----------------------------
suspend inline fun <reified T> HttpClient.getERPList(
    doctype: String,
    fields: List<String> = emptyList(),
    filters: List<Filter> = emptyList(),
    limit: Int? = null,
    offset: Int? = null,
    orderBy: String? = null,
    orderType: String = "desc",
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
): List<T> {
    require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

    val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}"

    val response: HttpResponse = this.get {
        url {
            takeFrom(endpoint)
            limit?.let { parameters.append("limit_page_length", it.toString()) }
            offset?.let { parameters.append("limit_start", it.toString()) }

            if (fields.isNotEmpty()) {
                // JSON array como string: ["item_code","item_name"]
                parameters.append("fields", json.encodeToString(fields))
            }

            if (filters.isNotEmpty()) {
                parameters.append("filters", buildFiltersJson(filters))
            }

            orderBy?.let {
                parameters.append("order_by", it)
                parameters.append("order_type", orderType)
            }
        }

        // Headers opcionales (Authorization, token, etc.)
        if (additionalHeaders.isNotEmpty()) {
            headers {
                additionalHeaders.forEach { (k, v) -> append(k, v) }
            }
        }

        // Prefer JSON
        accept(ContentType.Application.Json)
    }

    val responseBodyText = response.bodyAsText()
    if (!response.status.isSuccess()) {
        // Intentar parsear error de Frappe
        try {
            val err = json.decodeFromString<FrappeErrorResponse>(responseBodyText)
            throw FrappeException(err.exception ?: "Error: ${response.status}", err)
        } catch (e: Exception) {
            throw Exception("Error en petición: ${response.status} - $responseBodyText", e)
        }
    }

    // Parsear "data" y convertir a List<T>
    try {
        val parsed = json.parseToJsonElement(responseBodyText).jsonObject
        val dataElement = parsed["data"]
            ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $responseBodyText")
        // reified decodeFromJsonElement
        return json.decodeFromJsonElement(dataElement)
    } catch (e: Exception) {
        throw Exception(
            "Error parseando respuesta ERPNext: ${e.message}. Body: $responseBodyText",
            e
        )
    }
}

/** Sobrecarga: admitir DSL inline de filtros para limpieza en llamada */
suspend inline fun <reified T> HttpClient.getERPList(
    doctype: String,
    fields: List<String> = emptyList(),
    limit: Int? = null,
    offset: Int? = null,
    orderBy: String? = null,
    orderType: String = "desc",
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap(),
    block: (FiltersBuilder.() -> Unit)
): List<T> {
    require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

    val builtFilters = FiltersBuilder().apply(block).build()
    return getERPList(
        doctype = doctype,
        fields = fields,
        filters = builtFilters,
        limit = limit,
        offset = offset,
        orderBy = orderBy,
        orderType = orderType,
        baseUrl = baseUrl,
        additionalHeaders = additionalHeaders
    )
}

suspend inline fun <reified T> HttpClient.getERPSingle(
    doctype: String,
    name: String,
    baseUrl: String?,
    fields: List<String> = emptyList(),
    additionalHeaders: Map<String, String> = emptyMap()
): T {
    require(!baseUrl.isNullOrBlank()) { "baseUrl no puede ser nulo o vacío" }

    val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}/$name"

    val response: HttpResponse = this.get {
        url {
            takeFrom(endpoint)
            // Agregar campos específicos si se solicitan
            if (fields.isNotEmpty()) {
                parameters.append("fields", json.encodeToString(fields))
            }
        }

        // Headers opcionales (Auth, Token, etc.)
        if (additionalHeaders.isNotEmpty()) headers {
            additionalHeaders.forEach { (k, v) -> append(k, v) }
        }

        accept(ContentType.Application.Json)
    }

    val bodyText = response.bodyAsText()
    if (!response.status.isSuccess()) {
        try {
            val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
            throw FrappeException(err.exception ?: "Error: ${response.status}", err)
        } catch (e: Exception) {
            throw Exception("Error en petición: ${response.status} - $bodyText", e)
        }
    }

    val parsed = json.parseToJsonElement(bodyText).jsonObject
    val dataElement = parsed["data"]
        ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $bodyText")

    return json.decodeFromJsonElement<T>(dataElement)
}


suspend inline fun <reified T, reified R> HttpClient.postERP(
    doctype: String,
    payload: T,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
): R {
    try {
        require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

        val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}"
        val bodyText = this.post {
            url { takeFrom(endpoint) }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(payload))
            if (additionalHeaders.isNotEmpty()) headers {
                additionalHeaders.forEach { (k, v) ->
                    append(
                        k,
                        v
                    )
                }
            }
        }.bodyAsText()

        val parsed = json.parseToJsonElement(bodyText).jsonObject
        val dataElement = parsed["data"]
            ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $bodyText")
        return json.decodeFromJsonElement(dataElement)
    } catch (e: Exception) {
        e.printStackTrace()
        return ("" as R)
    }
}

suspend inline fun <reified T, reified R> HttpClient.putERP(
    doctype: String,
    name: String,
    payload: T,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
): R {
    require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

    val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}/${
        encodeURIComponent(name)
    }"
    val bodyText = this.put {
        url { takeFrom(endpoint) }
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(payload))
        if (additionalHeaders.isNotEmpty()) headers {
            additionalHeaders.forEach { (k, v) ->
                append(
                    k,
                    v
                )
            }
        }
    }.bodyAsText()

    val parsed = json.parseToJsonElement(bodyText).jsonObject
    val dataElement = parsed["data"]
        ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $bodyText")
    return json.decodeFromJsonElement(dataElement)
}

suspend fun HttpClient.deleteERP(
    doctype: String,
    name: String,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
) {
    require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

    val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}/${
        encodeURIComponent(name)
    }"
    val response = this.delete {
        url { takeFrom(endpoint) }
        if (additionalHeaders.isNotEmpty()) headers {
            additionalHeaders.forEach { (k, v) ->
                append(
                    k,
                    v
                )
            }
        }
    }

    if (!response.status.isSuccess()) {
        val body = response.bodyAsText()
        try {
            val err = json.decodeFromString<FrappeErrorResponse>(body)
            throw FrappeException(err.exception ?: "Error: ${response.status}", err)
        } catch (e: Exception) {
            throw Exception("Error en deleteERP: ${response.status} - $body", e)
        }
    }
}

// -----------------------------
// Util: encodeURIComponent simple
// -----------------------------
fun encodeURIComponent(value: String): String =
    value.encodeURLQueryComponent()
