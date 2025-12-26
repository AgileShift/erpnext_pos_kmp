package com.erpnext.pos.utils.oauth

actual class OAuthCallbackReceiver {
    actual fun start(redirectUrl: String): String = redirectUrl

    actual suspend fun awaitCode(expectedState: String): String = ""

    actual fun stop() {
    }
}
