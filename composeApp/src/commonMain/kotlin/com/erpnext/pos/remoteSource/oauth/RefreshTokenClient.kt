package com.erpnext.pos.remoteSource.oauth

import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode

suspend fun refreshAuthToken(
    client: HttpClient,
    authStore: AuthInfoStore,
    refreshToken: String
): TokenResponse {
    val normalizedRefreshToken = refreshToken.trim()
    require(normalizedRefreshToken.isNotBlank()) { "Missing refresh token" }
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
            append("refresh_token", normalizedRefreshToken)
            append("client_id", oauthConfig.clientId)
            oauthConfig.clientSecret.takeIf { it.isNotBlank() }?.let {
                append("client_secret", it)
            }
            oauthConfig.redirectUrl.takeIf { it.isNotBlank() }?.let {
                append("redirect_uri", it)
            }
        }.formUrlEncode()
        return client.post(config.tokenUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(params)
        }.body()
    } catch (e: ClientRequestException) {
        val responseText = runCatching { e.response.bodyAsText() }.getOrNull().orEmpty()
        AppSentry.capture(e, "refreshAuthToken failed ${e.response.status.value} $responseText")
        AppLogger.warn("refreshAuthToken failed ${e.response.status.value}: $responseText", e)
        throw e
    } catch (e: Throwable) {
        AppSentry.capture(e, "refreshAuthToken failed")
        AppLogger.warn("refreshAuthToken failed", e)
        throw e
    }
}

fun isRefreshTokenRejected(throwable: Throwable): Boolean {
    val requestException = throwable as? ClientRequestException ?: return false
    return when (requestException.response.status) {
        HttpStatusCode.BadRequest,
        HttpStatusCode.Unauthorized,
        HttpStatusCode.Forbidden -> true
        else -> false
    }
}
