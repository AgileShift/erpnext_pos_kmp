package com.erpnext.pos.remoteSource.api.v2

import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.OAuthConfig
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.toBearerToken
import com.erpnext.pos.remoteSource.sdk.Filter
import com.erpnext.pos.remoteSource.sdk.getERPList
import com.erpnext.pos.remoteSource.sdk.postERP
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.getFields
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode

data class DocCreateResponse(
    val name: String,
    val modified: String? = null
)

class APIServiceV2(
    private val client: HttpClient,
    private val store: TokenStore,
    private val authStore: AuthInfoStore
) {
    val clientOAuth = client
        .config {
            install(Auth) {
                bearer {
                    loadTokens {
                        store.load()?.toBearerToken()
                    }
                    refreshTokens {
                        val current = store.load() ?: return@refreshTokens null
                        val refreshed =
                            refreshToken(current.refresh_token ?: return@refreshTokens null)
                        val bearer = BearerTokens(
                            refreshed.access_token,
                            refreshed.refresh_token ?: current.refresh_token
                        )
                        store.save(
                            TokenResponse(
                                access_token = refreshed.access_token,
                                refresh_token = refreshed.refresh_token,
                                id_token = refreshed.id_token,
                                expires_in = refreshed.expires_in
                            )
                        )
                        bearer
                    }
                }
            }
        }

    suspend fun requireBaseUrl(): String {
        return authStore.getCurrentSite()
            ?: error("Missing ERPNext site URL in AuthInfoStore")
    }

    suspend inline fun <reified T> list(
        doctype: ERPDocType,
        fields: List<String> = doctype.getFields(),
        filters: List<Filter> = emptyList(),
        orderBy: String? = null,
        limit: Int? = null
    ): List<T> {
        return clientOAuth.getERPList(
            doctype = doctype.path,
            fields = fields,
            filters = filters,
            orderBy = orderBy,
            limit = limit,
            baseUrl = requireBaseUrl()
        )
    }

    suspend inline fun <reified T> createDoc(
        doctype: ERPDocType,
        payload: T
    ): DocCreateResponse {
        return clientOAuth.postERP(
            doctype = doctype.path,
            payload = payload,
            baseUrl = requireBaseUrl()
        )
    }

    private suspend fun refreshToken(refresh: String): TokenResponse {
        val currentSite = authStore.getCurrentSite()
        val oauthConfig = authStore.loadAuthInfoByUrl(currentSite!!)
        val config = OAuthConfig(
            oauthConfig.url,
            oauthConfig.clientId,
            oauthConfig.clientSecret,
            oauthConfig.redirectUrl,
            listOf("all", "openid")
        )
        return client.post(config.tokenUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refresh)
                    append("client_id", oauthConfig.clientId)
                    append("client_secret", oauthConfig.clientSecret)
                }.formUrlEncode()
            )
        }.body()
    }
}
