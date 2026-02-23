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
    val refreshScopes = oauthConfig.scopes
        .filterNot { it.equals("openid", ignoreCase = true) }
        .filter { it.isNotBlank() }
    val requestTrace = "site=${currentSite.trim()} tokenUrl=${config.tokenUrl} " +
            "grant_type=refresh_token client_id=${maskClientId(oauthConfig.clientId)} " +
            "client_secret_present=${oauthConfig.clientSecret.isNotBlank()} " +
            "redirect_uri=${maskRedirectUri(oauthConfig.redirectUrl)} " +
            "scope=${if (refreshScopes.isEmpty()) "<same-as-original>" else refreshScopes.joinToString(" ")} " +
            "refresh_token=${maskToken(normalizedRefreshToken)}"
    try {
        val params = Parameters.build {
            append("grant_type", "refresh_token")
            append("refresh_token", normalizedRefreshToken)
            append("client_id", oauthConfig.clientId)
            if (refreshScopes.isNotEmpty()) {
                append("scope", refreshScopes.joinToString(" "))
            }
            oauthConfig.clientSecret.takeIf { it.isNotBlank() }?.let {
                append("client_secret", it)
            }
            oauthConfig.redirectUrl.takeIf { it.isNotBlank() }?.let {
                append("redirect_uri", it)
            }
        }.formUrlEncode()
        AppLogger.info("OAuth refresh request -> $requestTrace")
        val response = client.post(config.tokenUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(params)
        }
        val tokenResponse = response.body<TokenResponse>()
        AppLogger.info(
            "OAuth refresh success -> status=${response.status.value} " +
                    "access_token=${maskToken(tokenResponse.access_token)} " +
                    "refresh_token=${maskToken(tokenResponse.refresh_token)} " +
                    "id_token=${maskToken(tokenResponse.id_token)} " +
                    "expires_in=${tokenResponse.expires_in}"
        )
        return tokenResponse
    } catch (e: ClientRequestException) {
        val responseText = runCatching { e.response.bodyAsText() }.getOrNull().orEmpty()
        val truncatedBody = responseText.take(MAX_ERROR_BODY_CHARS)
        AppSentry.capture(
            e,
            "refreshAuthToken failed status=${e.response.status.value} $requestTrace body=$truncatedBody"
        )
        AppLogger.warn(
            "OAuth refresh rejected -> status=${e.response.status.value} " +
                    "$requestTrace body=$truncatedBody",
            e
        )
        throw e
    } catch (e: Throwable) {
        AppSentry.capture(e, "refreshAuthToken failed $requestTrace")
        AppLogger.warn("OAuth refresh failed -> $requestTrace", e)
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

private const val MAX_ERROR_BODY_CHARS = 2000

private fun maskToken(value: String?): String {
    if (value.isNullOrBlank()) return "<empty>"
    val head = value.take(8)
    val tail = value.takeLast(6)
    return "len=${value.length} ${head}...${tail}"
}

private fun maskClientId(value: String?): String {
    if (value.isNullOrBlank()) return "<empty>"
    return "${value.take(6)}...(${value.length})"
}

private fun maskRedirectUri(value: String?): String {
    if (value.isNullOrBlank()) return "<empty>"
    val compact = value.trim()
    return if (compact.length <= 80) compact else "${compact.take(77)}..."
}
