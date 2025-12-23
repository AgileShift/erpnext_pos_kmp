package com.erpnext.pos.remoteSource.api.v2

import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import io.ktor.client.HttpClient

class APIServiceV2(
    private val client: HttpClient,
    private val store: TokenStore,
    private val authStore: AuthInfoStore
) {
    fun fetchCompanyInfo() {}

}