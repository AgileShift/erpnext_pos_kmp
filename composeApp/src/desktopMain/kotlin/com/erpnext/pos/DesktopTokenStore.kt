package com.erpnext.pos

import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.utils.TokenUtils.decodePayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import com.github.javakeyring.Keyring
import java.util.prefs.Preferences

class DesktopTokenStore(
    private val domain: String = "com.erpnext.pos",          // “service name” en el keyring
    prefsNode: String = "com.erpnext.pos.secure_prefs_v2"    // nodo Preferences
) : TokenStore, TransientAuthStore, AuthInfoStore {

    private val mutex = Mutex()
    private val stateFlow = MutableStateFlow<TokenResponse?>(null)
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: Preferences = Preferences.userRoot().node(prefsNode)

    // -------------------------
    // Keyring helpers
    // -------------------------
    private inline fun <T> withKeyring(block: (Keyring) -> T): T {
        val keyring = Keyring.create()
        try {
            return block(keyring)
        } catch (e: Exception) {
            throw IllegalStateException(
                "No se pudo acceder al Keyring del sistema en Desktop. " +
                        "Verifica soporte del SO/sesión (Linux) o empaquetado de la app.",
                e
            )
        } finally {
            try {
                keyring.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun setSecret(key: String, value: String) =
        withKeyring { it.setPassword(domain, key, value) }

    private fun getSecret(key: String): String? =
        withKeyring { it.getPassword(domain, key) }

    private fun deleteSecret(key: String) =
        withKeyring { it.deletePassword(domain, key) }

    // ------------------------------------------------------------
    // TokenStore
    // ------------------------------------------------------------
    override suspend fun save(tokens: TokenResponse) = mutex.withLock {
        clear()
        if (tokens.id_token.isNullOrEmpty()) return@withLock

        val claims = decodePayload(tokens.id_token)
        val userId = claims?.get("email").toString()

        setSecret("access_token", tokens.access_token)
        setSecret("refresh_token", tokens.refresh_token ?: "")
        setSecret("id_token", tokens.id_token)

        // metadatos: Preferences
        prefs.putLong("expires_in", tokens.expires_in ?: 0L)
        prefs.put("userId", userId)
        prefs.flush()

        stateFlow.value = tokens
    }

    override suspend fun load(): TokenResponse? = mutex.withLock {
        val at = getSecret("access_token") ?: return@withLock null
        val idToken = getSecret("id_token") ?: return@withLock null
        val rt = getSecret("refresh_token") ?: ""
        val expires = prefs.getLong("expires_in", 0L)

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
        prefs.get("userId", null)

    override suspend fun clear() = mutex.withLock {
        deleteSecret("access_token")
        deleteSecret("refresh_token")
        deleteSecret("id_token")

        prefs.remove("expires_in")
        prefs.remove("userId")
        prefs.flush()

        stateFlow.update { null }
    }

    override fun tokensFlow() = stateFlow.asStateFlow()

    // ------------------------------------------------------------
    // AuthInfoStore
    // ------------------------------------------------------------
    override suspend fun loadAuthInfoByUrl(url: String?): LoginInfo {
        val currentUrl = url?.takeIf { it.isNotBlank() } ?: getCurrentSite()
        val sitesInfo = loadAuthInfo()
        return sitesInfo.first { it.url == currentUrl }
    }

    override suspend fun loadAuthInfo(): MutableList<LoginInfo> {
        val raw = prefs.get("sitesInfo", null) ?: return mutableListOf()
        if (raw.isBlank()) return mutableListOf()
        return json.decodeFromString(raw)
    }

    override suspend fun saveAuthInfo(info: LoginInfo) = mutex.withLock {
        val list = loadAuthInfo()
        list.add(info)

        prefs.put("sitesInfo", json.encodeToString(list))
        prefs.put("current_site", info.url)
        prefs.flush()
    }

    override suspend fun getCurrentSite(): String? =
        prefs.get("current_site", null)

    override suspend fun clearAuthInfo() = mutex.withLock {
        prefs.remove("sitesInfo")
        prefs.flush()
    }

    // ------------------------------------------------------------
    // TransientAuthStore (yo lo guardo también en keyring)
    // ------------------------------------------------------------
    override suspend fun savePkceVerifier(verifier: String) {
        setSecret("pkce_verifier", verifier)
    }

    override suspend fun loadPkceVerifier(): String? =
        getSecret("pkce_verifier")

    override suspend fun clearPkceVerifier() {
        deleteSecret("pkce_verifier")
    }

    override suspend fun saveState(state: String) {
        setSecret("oauth_state", state)
    }

    override suspend fun loadState(): String? =
        getSecret("oauth_state")

    override suspend fun clearState() {
        deleteSecret("oauth_state")
    }
}
