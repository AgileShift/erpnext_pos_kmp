package com.erpnext.pos.utils.oauth

actual object OAuthRedirect {
    actual fun redirectUri(): String = "org.erpnext.pos://oauth2redirect"
}