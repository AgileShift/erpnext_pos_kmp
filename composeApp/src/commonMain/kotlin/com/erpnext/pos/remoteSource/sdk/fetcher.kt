package com.erpnext.pos.remoteSource.sdk

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.json.*
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import io.ktor.client.plugins.timeout
import kotlinx.coroutines.delay

// -----------------------------
// Configuración JSON global
// -----------------------------
val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun captureFetchError(context: String, throwable: Throwable) {
    AppSentry.capture(throwable, context)
    AppLogger.warn(context, throwable)
}

suspend inline fun <T> withRetries(
    retries: Int = 2,
    initialDelayMillis: Long = 500,
    block: () -> T
): T {
    var attempt = 0
    var delayMillis = initialDelayMillis
    while (true) {
        try {
            return block()
        } catch (e: Exception) {
            val retryable = e is ConnectTimeoutException || e is HttpRequestTimeoutException
            if (!retryable || attempt >= retries) throw e
            delay(delayMillis)
            attempt++
            delayMillis *= 2
        }
    }
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
    additionalHeaders: Map<String, String> = emptyMap(),
    noinline block: (FiltersBuilder.() -> Unit)? = null
): List<T> {
    require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

    val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}"
    val finalFilters = mutableListOf<Filter>().apply {
        addAll(filters)
        block?.let { addAll(FiltersBuilder().apply(it).build()) }
    }

    try {
        val response: HttpResponse = withRetries {
            this.get {
                url {
                    takeFrom(endpoint)
                    limit?.let { parameters.append("limit_page_length", it.toString()) }
                    offset?.let { parameters.append("limit_start", it.toString()) }

                    if (fields.isNotEmpty()) {
                        parameters.append("fields", json.encodeToString(fields))
                    }

                    if (finalFilters.isNotEmpty()) {
                        parameters.append("filters", buildFiltersJson(finalFilters))
                    }

                    orderBy?.let {
                        parameters.append("order_by", it)
                        parameters.append("order_type", orderType)
                    }
                }

                if (additionalHeaders.isNotEmpty()) {
                    headers {
                        additionalHeaders.forEach { (k, v) -> append(k, v) }
                    }
                }

                accept(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
            }
        }

        val responseBodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            try {
                val err = json.decodeFromString<FrappeErrorResponse>(responseBodyText)
                throw FrappeException(err.exception ?: "Error: ${response.status}", err)
            } catch (e: Exception) {
                throw Exception("Error en petición: ${response.status} - $responseBodyText", e)
            }
        }

        return try {
            val parsed = json.parseToJsonElement(responseBodyText).jsonObject
            val dataElement = parsed["data"]
                ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $responseBodyText")
            json.decodeFromJsonElement(dataElement)
        } catch (e: Exception) {
            throw Exception(
                "Error parseando respuesta ERPNext: ${e.message}. Body: $responseBodyText",
                e
            )
        }
    } catch (e: Exception) {
        captureFetchError("getERPList($doctype)", e)
        throw e
    }
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

    try {
        val response: HttpResponse = withRetries {
            this.get {
                url {
                    takeFrom(endpoint)
                    if (fields.isNotEmpty()) {
                        parameters.append("fields", json.encodeToString(fields))
                    }
                }

                if (additionalHeaders.isNotEmpty()) headers {
                    additionalHeaders.forEach { (k, v) -> append(k, v) }
                }

                accept(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
            }
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
    } catch (e: Exception) {
        captureFetchError("getERPSingle($doctype)", e)
        throw e
    }
}


suspend inline fun <reified T, reified R> HttpClient.postERP(
    doctype: String,
    payload: T,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
): R {
    require(baseUrl != null && baseUrl.isNotBlank()) { "baseUrl no puede ser nulo o vacío" }

    val endpoint = baseUrl.trimEnd('/') + "/api/resource/${encodeURIComponent(doctype)}"
    try {
        val bodyText = withRetries {
            this.post {
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
                timeout {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
            }.bodyAsText()
        }

        val parsed = json.parseToJsonElement(bodyText).jsonObject
        val dataElement = parsed["data"]
            ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $bodyText")
        return json.decodeFromJsonElement(dataElement)
    } catch (e: Exception) {
        captureFetchError("postERP($doctype)", e)
        throw e
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
    try {
        val bodyText = withRetries {
            this.put {
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
                timeout {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
            }.bodyAsText()
        }

        val parsed = json.parseToJsonElement(bodyText).jsonObject
        val dataElement = parsed["data"]
            ?: throw FrappeException("La respuesta no contiene 'data'. Respuesta: $bodyText")
        return json.decodeFromJsonElement(dataElement)
    } catch (e: Exception) {
        captureFetchError("putERP($doctype)", e)
        throw e
    }
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
    try {
        val response = withRetries {
            this.delete {
                url { takeFrom(endpoint) }
                if (additionalHeaders.isNotEmpty()) headers {
                    additionalHeaders.forEach { (k, v) ->
                        append(
                            k,
                            v
                        )
                    }
                }
                timeout {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
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
    } catch (e: Exception) {
        captureFetchError("deleteERP($doctype)", e)
        throw e
    }
}

// -----------------------------
// Util: encodeURIComponent simple
// -----------------------------
fun encodeURIComponent(value: String): String =
    value.encodeURLQueryComponent()
