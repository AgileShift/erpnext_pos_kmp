package com.erpnext.pos

import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.erpnext.pos.utils.TokenUtils.decodePayload
import kotlinx.cinterop.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.*
import platform.Security.*
import platform.darwin.*

@OptIn(ExperimentalForeignApi::class)
private fun keychainSet(key: String, value: String): Boolean {
    val data = value.cstr.getBytes()
    val query = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrAccount to key,
        kSecValueData to data
    )
    // delete existing
    SecItemDelete(query.toCFDictionary())
    val status = SecItemAdd(query.toCFDictionary(), null)
    return status == errSecSuccess
}

@OptIn(ExperimentalForeignApi::class)
private fun keychainGet(key: String): String? {
    val query = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrAccount to key,
        kSecReturnData to kCFBooleanTrue,
        kSecMatchLimit to kSecMatchLimitOne
    )
    val resultPtr = nativeHeap.alloc<COpaquePointerVar>()
    val status = SecItemCopyMatching(query.toCFDictionary(), resultPtr.ptr)
    if (status != errSecSuccess) {
        nativeHeap.free(resultPtr)
        return null
    }
    val data =
        resultPtr.value?.reinterpret<NSData>() ?: run { nativeHeap.free(resultPtr); return null }
    val str = NSString.create(data, 0u)!!.toString()
    nativeHeap.free(resultPtr)
    return str
}

@OptIn(ExperimentalForeignApi::class)
private fun keychainDelete(key: String) {
    val query = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrAccount to key
    )
    SecItemDelete(query.toCFDictionary())
}

class IosTokenStore : TokenStore, TransientAuthStore, AuthInfoStore {
    private val mutex = Mutex()
    private val _flow = MutableStateFlow<TokenResponse?>(null)
    private val json = Json { ignoreUnknownKeys = true }
    private val defaults = NSUserDefaults.standardUserDefaults

    private fun siteScopedKeyForUrl(url: String?, key: String): String {
        val siteKey = instanceKeyFromUrl(url)
        return "${siteKey}_$key"
    }

    private suspend fun siteScopedKey(key: String): String {
        return siteScopedKeyForUrl(getCurrentSite(), key)
    }

    private fun saveInternal(key: String, value: String) = keychainSet(key, value)
    private fun saveInternal(key: String, value: Long) = keychainSet(key, value)
    private fun loadInternal(key: String) = keychainGet(key)
    private fun deleteInternal(key: String) = keychainDelete(key)

    override suspend fun save(tokens: TokenResponse) = mutex.withLock {
        saveInternal(siteScopedKey("access_token"), tokens.access_token)
        saveInternal(siteScopedKey("refresh_token"), tokens.refresh_token ?: "")
        saveInternal(siteScopedKey("expires"), tokens.expires_in ?: 0L)
        saveInternal(siteScopedKey("id_token"), tokens.id_token ?: "")
        val claims = tokens.id_token?.let { decodePayload(it) }
        val userId = claims?.get("email")?.toString()
        userId?.let { defaults.setObject(it, forKey = siteScopedKey("userId")) }
        _flow.value = tokens
    }

    override suspend fun load(): TokenResponse? = mutex.withLock {
        val at = loadInternal(siteScopedKey("access_token")) ?: return null
        val rt = loadInternal(siteScopedKey("refresh_token")) ?: ""
        val expires = loadInternal(siteScopedKey("expires"))?.toLongOrNull()
        val idToken = loadInternal(siteScopedKey("id_token")) ?: return null
        val t = TokenResponse(
            access_token = at,
            refresh_token = rt,
            expires_in = expires,
            id_token = idToken
        )
        _flow.value = t
        t
    }

    override suspend fun clear() = mutex.withLock {
        deleteInternal(siteScopedKey("access_token"))
        deleteInternal(siteScopedKey("refresh_token"))
        deleteInternal(siteScopedKey("expires"))
        deleteInternal(siteScopedKey("id_token"))
        defaults.removeObjectForKey(siteScopedKey("userId"))
        _flow.value = null
    }

    override fun tokensFlow() = _flow.asStateFlow()

    override suspend fun loadUser(): String? =
        defaults.stringForKey(siteScopedKey("userId"))

    override suspend fun savePkceVerifier(verifier: String) {
        saveInternal("pkce_verifier", verifier)
    }

    override suspend fun loadPkceVerifier(): String? = loadInternal("pkce_verifier")
    override suspend fun clearPkceVerifier() = deleteInternal("pkce_verifier")

    override suspend fun saveState(state: String) = saveInternal("oauth_state", state)
    override suspend fun loadState(): String? = loadInternal("oauth_state")
    override suspend fun clearState() = deleteInternal("oauth_state")

    override suspend fun saveRedirectUri(uri: String) = saveInternal("oauth_redirect_uri", uri)

    override suspend fun loadRedirectUri(): String? = loadInternal("oauth_redirect_uri")

    override suspend fun clearRedirectUri() = deleteInternal("oauth_redirect_uri")

    // ------------------------------------------------------------
    // AuthInfoStore
    // ------------------------------------------------------------
    override suspend fun loadAuthInfo(): MutableList<LoginInfo> {
        val raw = defaults.stringForKey("sitesInfo") ?: return mutableListOf()
        if (raw.isBlank()) return mutableListOf()
        return json.decodeFromString(raw)
    }

    override suspend fun loadAuthInfoByUrl(url: String?, platform: String?): LoginInfo {
        val currentUrl = url?.takeIf { it.isNotBlank() } ?: getCurrentSite()
        val sitesInfo = loadAuthInfo()
        return sitesInfo.first { it.url == currentUrl }
    }

    override suspend fun saveAuthInfo(info: LoginInfo) = mutex.withLock {
        val list = loadAuthInfo()
        list.removeAll { it.url == info.url }
        val existing = list.firstOrNull { it.url == info.url }
        list.add(
            info.copy(
                lastUsedAt = existing?.lastUsedAt,
                isFavorite = existing?.isFavorite ?: info.isFavorite
            )
        )
        defaults.setObject(json.encodeToString(list), forKey = "sitesInfo")
        defaults.setObject(info.url, forKey = "current_site")
    }

    override suspend fun getCurrentSite(): String? =
        defaults.stringForKey("current_site")

    override suspend fun deleteSite(url: String): Boolean = mutex.withLock {
        val list = loadAuthInfo()
        if (list.none { it.url == url }) return@withLock false
        val updated = list.filterNot { it.url == url }

        deleteInternal(siteScopedKeyForUrl(url, "access_token"))
        deleteInternal(siteScopedKeyForUrl(url, "refresh_token"))
        deleteInternal(siteScopedKeyForUrl(url, "expires"))
        deleteInternal(siteScopedKeyForUrl(url, "id_token"))
        defaults.removeObjectForKey(siteScopedKeyForUrl(url, "userId"))

        val currentSite = getCurrentSite()
        if (currentSite == url) {
            _flow.value = null
            val nextSite = updated.firstOrNull()?.url
            if (nextSite.isNullOrBlank()) {
                defaults.removeObjectForKey("current_site")
            } else {
                defaults.setObject(nextSite, forKey = "current_site")
            }
        }

        if (updated.isEmpty()) {
            defaults.removeObjectForKey("sitesInfo")
        } else {
            defaults.setObject(json.encodeToString(updated), forKey = "sitesInfo")
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
        defaults.setObject(json.encodeToString(updated), forKey = "sitesInfo")
    }

    override suspend fun clearAuthInfo() = mutex.withLock {
        defaults.removeObjectForKey("sitesInfo")
        defaults.removeObjectForKey("current_site")
    }
}
