package com.erpnext.pos

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.utils.TokenUtils.decodePayload
import androidx.core.content.edit

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

    private fun stringKey(key: String) = key

    // ------------------------------------------------------------
    // TokenStore Implementation
    // ------------------------------------------------------------

    override suspend fun save(tokens: TokenResponse) {
        clear()
        if (tokens.id_token.isNullOrEmpty()) return
        val claims = decodePayload(tokens.id_token)
        val userId = claims?.get("email").toString()

        prefs.putString(stringKey("access_token"), tokens.access_token)
        prefs.putString(stringKey("refresh_token"), tokens.refresh_token ?: "")
        prefs.putString(stringKey("id_token"), tokens.id_token)
        prefs.putLong(stringKey("expires_in"), tokens.expires_in ?: 0L)
        prefs.putString(stringKey("userId"), userId)
        stateFlow.value = tokens
    }

    override suspend fun load(): TokenResponse? = mutex.withLock {
        val at = prefs.getString(stringKey("access_token")) ?: return null
        val rt = prefs.getString(stringKey("refresh_token")) ?: ""
        val expires = prefs.getLong(stringKey("expires_in"), 0L)
        val idToken = prefs.getString(stringKey("id_token")) ?: return null
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
        prefs.getString(stringKey("userId"))

    // ------------------------------------------------------------
    // AuthInfoStore Implementation
    // ------------------------------------------------------------

    override suspend fun loadAuthInfoByUrl(url: String?): LoginInfo {
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
        list.add(info)
        val serialized = json.encodeToString(list)
        prefs.putString("sitesInfo", serialized)
        prefs.putString("current_site", info.url)
    }

    override suspend fun getCurrentSite(): String? =
        prefs.getString(stringKey("current_site"))

    override suspend fun clearAuthInfo() = mutex.withLock {
        prefs.remove("sitesInfo")
    }

    override suspend fun clear() = mutex.withLock {
        prefs.remove("access_token")
        prefs.remove("refresh_token")
        prefs.remove("id_token")
        prefs.remove("expires_in")
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
