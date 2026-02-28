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
    private val desktopFallbackCompanies = setOf("ERPNext POS Desktop", "ERPNext POS Mobile")
    init {
        AppLogger.info(
            "DesktopTokenStore prefsNode=${prefs.absolutePath()} " +
                "currentSite=${prefs.get("current_site", null) ?: "none"}"
        )
    }

    private fun canonicalUrl(url: String?): String? =
        url?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }

    private fun isFallbackCompany(company: String?): Boolean {
        val normalized = company?.trim().orEmpty()
        return normalized.isBlank() || normalized in desktopFallbackCompanies
    }

    private fun mergeCompany(newCompany: String, existingCompany: String?): String {
        val newTrimmed = newCompany.trim()
        val existingTrimmed = existingCompany?.trim().orEmpty()
        if (newTrimmed.isBlank()) return existingTrimmed
        if (isFallbackCompany(newTrimmed) && !isFallbackCompany(existingTrimmed)) return existingTrimmed
        return newTrimmed
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
        AppLogger.info("DesktopTokenStore.save -> waiting mutex")
        mutex.withLock {
            AppLogger.info("DesktopTokenStore.save -> mutex acquired currentSite=${getCurrentSite()}")
            val currentAccessToken = getSecret(siteScopedKey("access_token"))
            val currentRefreshToken = getSecret(siteScopedKey("refresh_token"))
            val currentIdToken = getSecret(siteScopedKey("id_token"))
            val currentExpiresIn = prefs.getLong(siteScopedKey("expires_in"), 0L)
            val currentUser = prefs.get(siteScopedKey("userId"), null)

            val mergedAccessToken = tokens.access_token.ifBlank { currentAccessToken.orEmpty() }
            val mergedRefreshToken = tokens.refresh_token?.takeIf { it.isNotBlank() }
                ?: currentRefreshToken?.takeIf { it.isNotBlank() }
            val mergedIdToken = tokens.id_token?.takeIf { it.isNotBlank() }
                ?: currentIdToken?.takeIf { it.isNotBlank() }
            val mergedExpiresIn = tokens.expires_in ?: currentExpiresIn

            if (mergedAccessToken.isNotBlank()) {
                setSecret(siteScopedKey("access_token"), mergedAccessToken)
            } else {
                deleteSecret(siteScopedKey("access_token"))
            }
            if (mergedRefreshToken == null) {
                deleteSecret(siteScopedKey("refresh_token"))
            } else {
                setSecret(siteScopedKey("refresh_token"), mergedRefreshToken)
            }
            if (mergedIdToken == null) {
                deleteSecret(siteScopedKey("id_token"))
            } else {
                setSecret(siteScopedKey("id_token"), mergedIdToken)
            }

            prefs.putLong(siteScopedKey("expires_in"), mergedExpiresIn)
            val decodedUser = mergedIdToken?.let { resolveUserIdFromClaims(decodePayload(it)) }
                ?.takeIf { it.isNotBlank() }
                ?: currentUser
            if (decodedUser.isNullOrBlank()) {
                prefs.remove(siteScopedKey("userId"))
            } else {
                prefs.put(siteScopedKey("userId"), decodedUser)
            }
            prefs.flush()

            stateFlow.value = TokenResponse(
                access_token = mergedAccessToken,
                token_type = tokens.token_type,
                expires_in = mergedExpiresIn,
                refresh_token = mergedRefreshToken,
                id_token = mergedIdToken,
                scope = tokens.scope
            )
            AppLogger.info("DesktopTokenStore.save -> completed")
        }
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
        val currentUrl = canonicalUrl(url) ?: canonicalUrl(getCurrentSite())
        val sitesInfo = loadAuthInfo()
        val sameUrl = sitesInfo.filter { canonicalUrl(it.url) == currentUrl }
        if (getPlatformName() == "Desktop") {
            sameUrl.firstOrNull {
                it.redirectUrl.contains("127.0.0.1") && !isFallbackCompany(it.company)
            }?.let { return it }
            sameUrl.firstOrNull { !isFallbackCompany(it.company) }?.let { return it }
            sameUrl.firstOrNull { it.redirectUrl.contains("127.0.0.1") }?.let { return it }
        }
        sameUrl.firstOrNull()?.let { return it }
        return sitesInfo.firstOrNull()
            ?: throw NoSuchElementException("No auth info found for url=$currentUrl")
    }

    override suspend fun loadAuthInfo(): MutableList<LoginInfo> {
        val raw = prefs.get("sitesInfo", null) ?: return mutableListOf()
        if (raw.isBlank()) return mutableListOf()
        return json.decodeFromString(raw)
    }

    override suspend fun saveAuthInfo(info: LoginInfo) = mutex.withLock {
        val previousSite = prefs.get("current_site", null)
        val list = loadAuthInfo()
        val infoCanonicalUrl = canonicalUrl(info.url) ?: info.url
        val sameUrlEntries = list.filter { canonicalUrl(it.url) == infoCanonicalUrl }
        val existing = sameUrlEntries.firstOrNull()
        list.removeAll { canonicalUrl(it.url) == infoCanonicalUrl }

        val mergedCompany = mergeCompany(
            newCompany = info.company,
            existingCompany = sameUrlEntries.firstOrNull { !isFallbackCompany(it.company) }?.company
                ?: existing?.company
        )
        list.add(
            info.copy(
                url = infoCanonicalUrl,
                company = mergedCompany,
                lastUsedAt = existing?.lastUsedAt,
                isFavorite = existing?.isFavorite ?: info.isFavorite
            )
        )

        prefs.put("sitesInfo", json.encodeToString(list))
        prefs.put("current_site", infoCanonicalUrl)
        prefs.flush()
        AppLogger.info(
            "DesktopTokenStore.saveAuthInfo -> current_site ${previousSite ?: "none"} -> $infoCanonicalUrl company=${mergedCompany.ifBlank { "none" }}"
        )
    }

    override suspend fun getCurrentSite(): String? =
        prefs.get("current_site", null)

    override suspend fun deleteSite(url: String): Boolean = mutex.withLock {
        val list = loadAuthInfo()
        val targetUrl = canonicalUrl(url) ?: url
        if (list.none { canonicalUrl(it.url) == targetUrl }) return@withLock false
        val matchingEntries = list.filter { canonicalUrl(it.url) == targetUrl }
        val updated = list.filterNot { canonicalUrl(it.url) == targetUrl }

        matchingEntries.forEach { entry ->
            deleteSecret(siteScopedKeyForUrl(entry.url, "access_token"))
            deleteSecret(siteScopedKeyForUrl(entry.url, "refresh_token"))
            deleteSecret(siteScopedKeyForUrl(entry.url, "id_token"))
            prefs.remove(siteScopedKeyForUrl(entry.url, "expires_in"))
            prefs.remove(siteScopedKeyForUrl(entry.url, "userId"))
        }

        val currentSite = canonicalUrl(getCurrentSite())
        if (currentSite == targetUrl) {
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
        AppLogger.info("DesktopTokenStore.updateSiteMeta -> start url=$url")
        val targetUrl = canonicalUrl(url) ?: url
        val list = loadAuthInfo()
        val updated = list.map { item ->
            if (canonicalUrl(item.url) != targetUrl) return@map item
            item.copy(
                lastUsedAt = lastUsedAt ?: item.lastUsedAt,
                isFavorite = isFavorite ?: item.isFavorite
            )
        }
        prefs.put("sitesInfo", json.encodeToString(updated))
        prefs.flush()
        AppLogger.info("DesktopTokenStore.updateSiteMeta -> done url=$url")
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
