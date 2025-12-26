package com.erpnext.pos.utils.oauth

expect object OAuthRedirect {
    fun redirectUri(): String
}