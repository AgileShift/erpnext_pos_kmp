package com.erpnext.pos.remoteSource.oauth

import com.erpnext.pos.base64UrlNoPad
import com.erpnext.pos.randomUrlSafe
import com.erpnext.pos.sha256

data class Pkce(val verifier: String, val challenge: String, val method: String = "S256")

object PkceFactory {
    fun s256(): Pkce {
        val verifier = randomUrlSafe(64)
        val challenge = base64UrlNoPad(sha256(verifier.encodeToByteArray()))
        return Pkce(verifier, challenge)
    }
}