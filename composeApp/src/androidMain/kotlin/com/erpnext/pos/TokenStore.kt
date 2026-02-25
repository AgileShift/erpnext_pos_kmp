package com.erpnext.pos

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.utils.TokenUtils.decodePayload
import com.erpnext.pos.utils.TokenUtils.resolveUserIdFromClaims
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * AndroidTokenStore
 * KMP-compatible secure token storage using Tink AES-GCM encryption.
 */
class AndroidTokenStore(private val context: Context) : TokenStore, TransientAuthStore,
    AuthInfoStore {

    private val mutex = Mutex()
    private val stateFlow = MutableStateFlow<TokenResponse?>(null)
    private val json = Json { ignoreUnknownKeys = true }

    // --- SecurePrefs initialization ---
    private val prefs by lazy {
        TinkConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "master_keyset", "master_key_preference")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://master_key")
            .build()
            .keysetHandle

        val aead = keysetHandle.getPrimitive(Aead::class.java)
        SecurePrefs(context, "secure_prefs_v2", aead)
    }

    private fun siteScopedKeyForUrl(url: String?, key: String): String {
        val siteKey = instanceKeyFromUrl(url)
        return "${siteKey}_$key"
    }

    private suspend fun siteScopedKey(key: String): String {
        return siteScopedKeyForUrl(getCurrentSite(), key)
    }

    // ------------------------------------------------------------
    // TokenStore Implementation
    // ------------------------------------------------------------

    override suspend fun save(tokens: TokenResponse) {
        mutex.withLock {
            val currentAccessToken = prefs.getString(siteScopedKey("access_token"))
            val currentRefreshToken = prefs.getString(siteScopedKey("refresh_token"))
            val currentIdToken = prefs.getString(siteScopedKey("id_token"))
            val currentExpiresIn = prefs.getLong(siteScopedKey("expires_in"), 0L)
            val currentUser = prefs.getString(siteScopedKey("userId"))

            val mergedRefreshToken = tokens.refresh_token?.takeIf { it.isNotBlank() }
                ?: currentRefreshToken?.takeIf { it.isNotBlank() }
            val mergedIdToken = tokens.id_token?.takeIf { it.isNotBlank() }
                ?: currentIdToken?.takeIf { it.isNotBlank() }
            val mergedExpiresIn = tokens.expires_in ?: currentExpiresIn
            val mergedAccessToken = tokens.access_token.ifBlank { currentAccessToken.orEmpty() }

            if (mergedAccessToken.isNotBlank()) {
                prefs.putString(siteScopedKey("access_token"), mergedAccessToken)
            } else {
                prefs.remove(siteScopedKey("access_token"))
            }
            if (mergedRefreshToken == null) {
                prefs.remove(siteScopedKey("refresh_token"))
            } else {
                prefs.putString(siteScopedKey("refresh_token"), mergedRefreshToken)
            }
            if (mergedIdToken == null) {
                prefs.remove(siteScopedKey("id_token"))
            } else {
                prefs.putString(siteScopedKey("id_token"), mergedIdToken)
            }
            prefs.putLong(siteScopedKey("expires_in"), mergedExpiresIn)

            val userId = mergedIdToken?.let { id ->
                resolveUserIdFromClaims(decodePayload(id)).orEmpty()
            }?.takeIf { it.isNotBlank() } ?: currentUser
            if (userId.isNullOrBlank()) {
                prefs.remove(siteScopedKey("userId"))
            } else {
                prefs.putString(siteScopedKey("userId"), userId)
            }

            stateFlow.value = TokenResponse(
                access_token = mergedAccessToken,
                token_type = tokens.token_type,
                expires_in = mergedExpiresIn,
                refresh_token = mergedRefreshToken,
                id_token = mergedIdToken,
                scope = tokens.scope
            )
        }
    }

    override suspend fun load(): TokenResponse? = mutex.withLock {
        val at = prefs.getString(siteScopedKey("access_token")) ?: return null
        val rt = prefs.getString(siteScopedKey("refresh_token"))?.takeIf { it.isNotBlank() }
        val expires = prefs.getLong(siteScopedKey("expires_in"), 0L)
        val idToken = prefs.getString(siteScopedKey("id_token")) ?: return null
        val tokens = TokenResponse(
            access_token = at,
            refresh_token = rt,
            expires_in = expires,
            id_token = idToken
        )
        stateFlow.value = tokens
        tokens
    }

    override suspend fun loadUser(): String? =
        prefs.getString(siteScopedKey("userId"))

    // ------------------------------------------------------------
    // AuthInfoStore Implementation
    // ------------------------------------------------------------

    override suspend fun loadAuthInfoByUrl(url: String?, platform: String?): LoginInfo {
        var currentUrl = url
        if (currentUrl.isNullOrEmpty())
            currentUrl = getCurrentSite()
        val sitesInfo = loadAuthInfo()
        return sitesInfo.first { info -> info.url == currentUrl }
    }

    override suspend fun loadAuthInfo(): MutableList<LoginInfo> {
        val sitesInfo = prefs.getString("sitesInfo")
        if (sitesInfo.isNullOrEmpty()) return mutableListOf()
        return json.decodeFromString(sitesInfo)
    }

    override suspend fun saveAuthInfo(info: LoginInfo) = mutex.withLock {
        val list = loadAuthInfo()
        val existing = list.firstOrNull { it.url == info.url }
        list.removeAll { it.url == info.url }
        list.add(
            info.copy(
                lastUsedAt = existing?.lastUsedAt,
                isFavorite = existing?.isFavorite ?: info.isFavorite
            )
        )
        val serialized = json.encodeToString(list)
        prefs.putString("sitesInfo", serialized)
        prefs.putString("current_site", info.url)
    }

    override suspend fun getCurrentSite(): String? =
        prefs.getString("current_site")

    override suspend fun deleteSite(url: String): Boolean = mutex.withLock {
        val list = loadAuthInfo()
        if (list.none { it.url == url }) return@withLock false
        val updated = list.filterNot { it.url == url }
        prefs.remove(siteScopedKeyForUrl(url, "access_token"))
        prefs.remove(siteScopedKeyForUrl(url, "refresh_token"))
        prefs.remove(siteScopedKeyForUrl(url, "id_token"))
        prefs.remove(siteScopedKeyForUrl(url, "expires_in"))
        prefs.remove(siteScopedKeyForUrl(url, "userId"))

        val currentSite = getCurrentSite()
        if (currentSite == url) {
            stateFlow.update { null }
            val nextSite = updated.firstOrNull()?.url
            if (nextSite.isNullOrBlank()) {
                prefs.remove("current_site")
            } else {
                prefs.putString("current_site", nextSite)
            }
        }
        if (updated.isEmpty()) {
            prefs.remove("sitesInfo")
        } else {
            prefs.putString("sitesInfo", json.encodeToString(updated))
        }
        true
    }

    override suspend fun updateSiteMeta(
        url: String,
        lastUsedAt: Long?,
        isFavorite: Boolean?
    ) = mutex.withLock {
        val list = loadAuthInfo()
        val updated = list.map { item ->
            if (item.url != url) return@map item
            item.copy(
                lastUsedAt = lastUsedAt ?: item.lastUsedAt,
                isFavorite = isFavorite ?: item.isFavorite
            )
        }
        prefs.putString("sitesInfo", json.encodeToString(updated))
    }

    override suspend fun clearAuthInfo() = mutex.withLock {
        prefs.remove("sitesInfo")
        prefs.remove("current_site")
    }

    override suspend fun clear() = mutex.withLock {
        prefs.remove(siteScopedKey("access_token"))
        prefs.remove(siteScopedKey("refresh_token"))
        prefs.remove(siteScopedKey("id_token"))
        prefs.remove(siteScopedKey("expires_in"))
        prefs.remove(siteScopedKey("userId"))
        stateFlow.update { null }
    }

    override fun tokensFlow() = stateFlow.asStateFlow()

    // ------------------------------------------------------------
    // TransientAuthStore Implementation
    // ------------------------------------------------------------

    override suspend fun savePkceVerifier(verifier: String) {
        prefs.putString("pkce_verifier", verifier)
    }

    override suspend fun loadPkceVerifier(): String? =
        prefs.getString("pkce_verifier")

    override suspend fun clearPkceVerifier() {
        prefs.remove("pkce_verifier")
    }

    override suspend fun saveState(state: String) {
        prefs.putString("oauth_state", state)
    }

    override suspend fun loadState(): String? =
        prefs.getString("oauth_state")

    override suspend fun clearState() {
        prefs.remove("oauth_state")
    }

    override suspend fun saveRedirectUri(uri: String) {
        prefs.putString("oauth_redirect_uri", uri)
    }

    override suspend fun loadRedirectUri(): String? =
        prefs.getString("oauth_redirect_uri")

    override suspend fun clearRedirectUri() {
        prefs.remove("oauth_redirect_uri")
    }

    // ------------------------------------------------------------
    // Internal helper class (Android only)
    // ------------------------------------------------------------

    private class SecurePrefs(
        context: Context,
        name: String,
        private val aead: Aead
    ) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(name, Context.MODE_PRIVATE)

        fun putString(key: String, value: String) {
            val encrypted = try {
                val bytes = aead.encrypt(value.toByteArray(), null)
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Exception) {
                ""
            }
            prefs.edit { putString(key, encrypted) }
        }

        fun getString(key: String, default: String? = null): String? {
            val encrypted = prefs.getString(key, null) ?: return default
            return try {
                val decrypted = aead.decrypt(Base64.decode(encrypted, Base64.NO_WRAP), null)
                String(decrypted)
            } catch (_: Exception) {
                default
            }
        }

        fun putLong(key: String, value: Long) {
            putString(key, value.toString())
        }

        fun getLong(key: String, default: Long = 0L): Long {
            return getString(key)?.toLongOrNull() ?: default
        }

        fun remove(key: String) {
            prefs.edit { remove(key) }
        }

        fun clear() {
            prefs.edit { clear() }
        }
    }
}
