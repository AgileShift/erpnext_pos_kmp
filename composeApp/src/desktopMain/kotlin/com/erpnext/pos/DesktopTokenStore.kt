package com.erpnext.pos

import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.TokenUtils.decodePayload
import com.erpnext.pos.utils.TokenUtils.resolveUserIdFromClaims
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.github.javakeyring.Keyring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

class DesktopTokenStore(
    private val domain: String = "com.erpnext.pos",
    prefsNode: String = "com.erpnext.pos.secure_prefs_v2"
) : TokenStore, TransientAuthStore, AuthInfoStore {

    private val mutex = Mutex()
    private val stateFlow = MutableStateFlow<TokenResponse?>(null)
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: Preferences = Preferences.userRoot().node(prefsNode)
    init {
        AppLogger.info(
            "DesktopTokenStore prefsNode=${prefs.absolutePath()} " +
                "currentSite=${prefs.get("current_site", null) ?: "none"}"
        )
    }

    /**
     * Keyring best-effort:
     * - Si falla por permisos/entorno/IDE, NO tumba la app.
     * - Degrada a Preferences (menos seguro, pero evita crash).
     */
    @Volatile
    private var keyringOk: Boolean = probeKeyring()

    private fun probeKeyring(): Boolean {
        return runCatching {
            val kr = Keyring.create()
            try {
                val probeKey = "__probe__"
                kr.setPassword(domain, probeKey, "ok")
                kr.getPassword(domain, probeKey)
                kr.deletePassword(domain, probeKey)
            } finally {
                runCatching { kr.close() }
            }
            true
        }.getOrElse { e ->
            // No rompas el arranque por esto; solo log.
            // Puedes reemplazar por tu logger.
            println(
                "WARN: Keyring no disponible en Desktop. Fallback a Preferences. " +
                        "${e.javaClass.simpleName}: ${e.message}"
            )
            false
        }
    }

    private fun prefKey(key: String) = "secret.$key"
    private fun siteScopedKeyForUrl(url: String?, key: String): String {
        val siteKey = instanceKeyFromUrl(url)
        return "${siteKey}_$key"
    }

    private suspend fun siteScopedKey(key: String): String {
        return siteScopedKeyForUrl(getCurrentSite(), key)
    }

    private inline fun <T> runKeyring(block: (Keyring) -> T): Result<T> {
        return runCatching {
            val kr = Keyring.create()
            try {
                block(kr)
            } finally {
                runCatching { kr.close() }
            }
        }
    }

    private fun setSecret(key: String, value: String) {
        if (keyringOk) {
            val ok = runKeyring { it.setPassword(domain, key, value) }.isSuccess
            if (ok) return
            keyringOk = false
        }
        // Fallback (menos seguro, pero estable)
        prefs.put(prefKey(key), value)
        prefs.flush()
    }

    private fun getSecret(key: String): String? {
        if (keyringOk) {
            val res = runKeyring { it.getPassword(domain, key) }
            if (res.isSuccess) return res.getOrNull()
            keyringOk = false
        }
        return prefs.get(prefKey(key), null)
    }

    private fun deleteSecret(key: String) {
        if (keyringOk) {
            val ok = runKeyring { it.deletePassword(domain, key) }.isSuccess
            if (ok) return
            keyringOk = false
        }
        prefs.remove(prefKey(key))
        prefs.flush()
    }

    // ------------------------------------------------------------
    // TokenStore
    // ------------------------------------------------------------
    override suspend fun save(tokens: TokenResponse) {
        clear()
        if (tokens.id_token.isNullOrEmpty()) return

        val claims = decodePayload(tokens.id_token)
        val userId = resolveUserIdFromClaims(claims).orEmpty()

        // secretos
        setSecret(siteScopedKey("access_token"), tokens.access_token)
        val refreshToken = tokens.refresh_token?.takeIf { it.isNotBlank() }
        if (refreshToken == null) {
            deleteSecret(siteScopedKey("refresh_token"))
        } else {
            setSecret(siteScopedKey("refresh_token"), refreshToken)
        }
        setSecret(siteScopedKey("id_token"), tokens.id_token)

        // metadatos (no secretos)
        prefs.putLong(siteScopedKey("expires_in"), tokens.expires_in ?: 0L)
        prefs.put(siteScopedKey("userId"), userId)
        prefs.flush()

        stateFlow.value = tokens
    }

    override suspend fun load(): TokenResponse? = mutex.withLock {
        // si falla keyring, getSecret() retorna null o usa fallback, sin crashear
        val at = getSecret(siteScopedKey("access_token")) ?: return@withLock null
        val idToken = getSecret(siteScopedKey("id_token")) ?: return@withLock null
        val rt = getSecret(siteScopedKey("refresh_token"))?.takeIf { it.isNotBlank() }
        val expires = prefs.getLong(siteScopedKey("expires_in"), 0L)

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
        prefs.get(siteScopedKey("userId"), null)

    override suspend fun clear() = mutex.withLock {
        deleteSecret(siteScopedKey("access_token"))
        deleteSecret(siteScopedKey("refresh_token"))
        deleteSecret(siteScopedKey("id_token"))

        prefs.remove(siteScopedKey("expires_in"))
        prefs.remove(siteScopedKey("userId"))
        prefs.flush()

        stateFlow.update { null }
    }

    override fun tokensFlow() = stateFlow.asStateFlow()

    // ------------------------------------------------------------
    // AuthInfoStore
    // ------------------------------------------------------------
    override suspend fun loadAuthInfoByUrl(url: String?, platform: String?): LoginInfo {
        val currentUrl = url?.takeIf { it.isNotBlank() } ?: getCurrentSite()
        val sitesInfo = loadAuthInfo()
        return if (getPlatformName() == "Desktop")
            sitesInfo.first { it.url == currentUrl && it.redirectUrl.contains("127.0.0.1") }
        else sitesInfo.first { it.url == currentUrl }
    }

    override suspend fun loadAuthInfo(): MutableList<LoginInfo> {
        val raw = prefs.get("sitesInfo", null) ?: return mutableListOf()
        if (raw.isBlank()) return mutableListOf()
        return json.decodeFromString(raw)
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

        prefs.put("sitesInfo", json.encodeToString(list))
        prefs.put("current_site", info.url)
        prefs.flush()
    }

    override suspend fun getCurrentSite(): String? =
        prefs.get("current_site", null)

    override suspend fun deleteSite(url: String): Boolean = mutex.withLock {
        val list = loadAuthInfo()
        if (list.none { it.url == url }) return@withLock false
        val updated = list.filterNot { it.url == url }

        deleteSecret(siteScopedKeyForUrl(url, "access_token"))
        deleteSecret(siteScopedKeyForUrl(url, "refresh_token"))
        deleteSecret(siteScopedKeyForUrl(url, "id_token"))
        prefs.remove(siteScopedKeyForUrl(url, "expires_in"))
        prefs.remove(siteScopedKeyForUrl(url, "userId"))

        val currentSite = getCurrentSite()
        if (currentSite == url) {
            stateFlow.update { null }
            val nextSite = updated.firstOrNull()?.url
            if (nextSite.isNullOrBlank()) {
                prefs.remove("current_site")
            } else {
                prefs.put("current_site", nextSite)
            }
        }

        if (updated.isEmpty()) {
            prefs.remove("sitesInfo")
        } else {
            prefs.put("sitesInfo", json.encodeToString(updated))
        }
        prefs.flush()
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
        prefs.put("sitesInfo", json.encodeToString(updated))
        prefs.flush()
    }

    override suspend fun clearAuthInfo() = mutex.withLock {
        prefs.remove("sitesInfo")
        prefs.remove("current_site")
        prefs.flush()
    }

    // ------------------------------------------------------------
    // TransientAuthStore
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

    override suspend fun saveRedirectUri(uri: String) {
        setSecret("oauth_redirect_uri", uri)
    }

    override suspend fun loadRedirectUri(): String? =
        getSecret("oauth_redirect_uri")

    override suspend fun clearRedirectUri() {
        deleteSecret("oauth_redirect_uri")
    }
}
