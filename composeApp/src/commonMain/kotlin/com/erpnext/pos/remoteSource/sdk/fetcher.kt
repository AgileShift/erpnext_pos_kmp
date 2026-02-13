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
    doctype
    fields
    filters
    limit
    offset
    orderBy
    orderType
    baseUrl
    additionalHeaders
    block
    throw UnsupportedOperationException(
        "getERPList deshabilitado: usa endpoints API v1/sync.bootstrap."
    )
}

suspend inline fun <reified T> HttpClient.getERPSingle(
    doctype: String,
    name: String,
    baseUrl: String?,
    fields: List<String> = emptyList(),
    additionalHeaders: Map<String, String> = emptyMap()
): T {
    doctype
    name
    baseUrl
    fields
    additionalHeaders
    throw UnsupportedOperationException(
        "getERPSingle deshabilitado: usa endpoints API v1/sync.bootstrap."
    )
}


suspend inline fun <reified T, reified R> HttpClient.postERP(
    doctype: String,
    payload: T,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
): R {
    doctype
    payload
    baseUrl
    additionalHeaders
    throw UnsupportedOperationException(
        "postERP deshabilitado: usa endpoints API v1/sync.bootstrap."
    )
}

suspend inline fun <reified T, reified R> HttpClient.putERP(
    doctype: String,
    name: String,
    payload: T,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
): R {
    doctype
    name
    payload
    baseUrl
    additionalHeaders
    throw UnsupportedOperationException(
        "putERP deshabilitado: usa endpoints API v1/sync.bootstrap."
    )
}

suspend fun HttpClient.deleteERP(
    doctype: String,
    name: String,
    baseUrl: String?,
    additionalHeaders: Map<String, String> = emptyMap()
) {
    doctype
    name
    baseUrl
    additionalHeaders
    throw UnsupportedOperationException(
        "deleteERP deshabilitado: usa endpoints API v1/sync.bootstrap."
    )
}

// -----------------------------
// Util: encodeURIComponent simple
// -----------------------------
fun encodeURIComponent(value: String): String =
    value.encodeURLQueryComponent()
