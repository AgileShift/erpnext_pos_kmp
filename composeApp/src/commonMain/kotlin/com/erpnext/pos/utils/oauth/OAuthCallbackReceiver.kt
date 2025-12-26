package com.erpnext.pos.utils.oauth

expect class OAuthCallbackReceiver() {
    fun start(): String               // retorna redirectUri
    suspend fun awaitCode(expectedState: String): String
    fun stop()
}