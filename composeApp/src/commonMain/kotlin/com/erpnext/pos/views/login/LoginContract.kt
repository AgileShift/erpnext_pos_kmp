package com.erpnext.pos.views.login

import com.erpnext.pos.remoteSource.dto.TokenResponse
import kotlinx.serialization.Serializable

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val sites: List<Site>? = null) : LoginState()
    data class Authenticated(val tokens: TokenResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}

data class LoginAction(
    val existingSites: () -> Unit = { },
    val onSiteSelected: (site: Site) -> Unit = { },
    val onToggleFavorite: (site: Site) -> Unit = { },
    val onDeleteSite: (site: Site) -> Unit = { },
    val onAddSite: (String) -> Unit = {},
    val isAuthenticated: (TokenResponse) -> Unit = { },
    val onError: (error: String) -> Unit = {},
    val clear: () -> Unit = {},
    val onReset: () -> Unit = {}
)

@Serializable
data class Site(
    val url: String,
    val name: String,
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false
)
