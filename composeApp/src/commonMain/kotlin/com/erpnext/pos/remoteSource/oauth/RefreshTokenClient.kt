package com.erpnext.pos.remoteSource.oauth

import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode

suspend fun refreshAuthToken(
    client: HttpClient,
    authStore: AuthInfoStore,
    refreshToken: String
): TokenResponse {
    val currentSite = authStore.getCurrentSite()
        ?: throw IllegalStateException("Missing ERPNext site URL in AuthInfoStore")
    val oauthConfig = authStore.loadAuthInfoByUrl(currentSite)
    val config = OAuthConfig(
        oauthConfig.url,
        oauthConfig.clientId,
        oauthConfig.clientSecret,
        oauthConfig.redirectUrl,
        listOf("all", "openid")
    )
    try {
        val params = Parameters.build {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
            append("client_id", oauthConfig.clientId)
            oauthConfig.clientSecret.takeIf { it.isNotBlank() }?.let {
                append("client_secret", it)
            }
            oauthConfig.redirectUrl.takeIf { it.isNotBlank() }?.let {
                append("redirect_uri", it)
            }
            val scopes = oauthConfig.scopes
            if (scopes.isNotEmpty()) {
                append("scope", scopes.joinToString(" "))
            }
        }.formUrlEncode()
        return client.post(config.tokenUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(params)
        }.body()
    } catch (e: Throwable) {
        AppSentry.capture(e, "refreshAuthToken failed")
        AppLogger.warn("refreshAuthToken failed", e)
        throw e
    }
}
