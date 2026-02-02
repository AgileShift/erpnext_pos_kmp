package com.erpnext.pos.remoteSource.oauth

data class OAuthConfig(
    val baseUrl: String,
    val clientId: String,
    val clientSecret: String? = null,
    val redirectUrl: String,
    val scopes: List<String> = listOf("all", "openid")
) {
    val authorizeUrl = "$baseUrl/api/method/frappe.integrations.oauth2.authorize"
    val tokenUrl = "$baseUrl/api/method/frappe.integrations.oauth2.get_token"
    val revokeUrl = "$baseUrl/api/method/frappe.integrations.oauth2.revoke_token"
}
