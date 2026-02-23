package com.erpnext.pos.remoteSource.sdk

import io.ktor.client.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import io.ktor.http.encodeURLQueryComponent
import kotlinx.serialization.json.Json
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
// Util: encodeURIComponent simple
// -----------------------------
fun encodeURIComponent(value: String): String =
    value.encodeURLQueryComponent()
