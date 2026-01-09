package com.erpnext.pos.remoteSource.oauth

import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.OAuthConfig
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
        return client.post(config.tokenUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                    append("client_id", oauthConfig.clientId)
                    append("client_secret", oauthConfig.clientSecret)
                }.formUrlEncode()
            )
        }.body()
    } catch (e: Throwable) {
        AppSentry.capture(e, "refreshAuthToken failed")
        AppLogger.warn("refreshAuthToken failed", e)
        throw e
    }
}
