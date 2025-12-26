package com.erpnext.pos.utils.oauth

actual class OAuthCallbackReceiver {
    actual fun start(): String = "org.erpnext.pos://oauth2redirect"

    actual suspend fun awaitCode(expectedState: String): String = ""

    actual fun stop() {
    }
}