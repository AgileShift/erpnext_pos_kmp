package com.erpnext.pos

import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import kotlinx.cinterop.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    return status == errSecSuccessL
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

class IosTokenStore : TokenStore, TransientAuthStore {
    private val mutex = Mutex()
    private val _flow = MutableStateFlow<TokenResponse?>(null)

    private fun saveInternal(key: String, value: String) = keychainSet(key, value)
    private fun saveInternal(key: String, value: Long) = keychainSet(key, value)
    private fun loadInternal(key: String) = keychainGet(key)
    private fun deleteInternal(key: String) = keychainDelete(key)

    override suspend fun save(tokens: TokenResponse) = mutex.withLock {
        saveInternal("access_token", tokens.access_token)
        saveInternal("refresh_token", tokens.refresh_token ?: "")
        saveInternal("expires", tokens.expires_in ?: 0L)
        saveInternal("id_token", tokens.id_token ?: "")
        _flow.value = tokens
    }

    override suspend fun load(): TokenResponse? = mutex.withLock {
        val at = loadInternal("access_token") ?: return null
        val rt = loadInternal("refresh_token") ?: ""
        val expires = loadInternal("expires")?.toLongOrNull()
        val idToken = loadInternal("id_token") ?: return null
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
        deleteInternal("access_token")
        deleteInternal("refresh_token")
        deleteInternal("expires_at")
        _flow.value = null
    }

    override fun tokensFlow() = _flow.asStateFlow()

    override suspend fun savePkceVerifier(verifier: String) {
        saveInternal("pkce_verifier", verifier)
    }

    override suspend fun loadPkceVerifier(): String? = loadInternal("pkce_verifier")
    override suspend fun clearPkceVerifier() = deleteInternal("pkce_verifier")

    override suspend fun saveState(state: String) = saveInternal("oauth_state", state)
    override suspend fun loadState(): String? = loadInternal("oauth_state")
    override suspend fun clearState() = deleteInternal("oauth_state")
}
