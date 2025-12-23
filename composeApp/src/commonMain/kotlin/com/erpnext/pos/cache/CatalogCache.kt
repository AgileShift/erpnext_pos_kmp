package com.erpnext.pos.cache

import com.erpnext.pos.remoteSource.dto.v2.CatalogSnapshot

class CatalogCache {
    private var cacheKey: String? = null
    private var snapshot: CatalogSnapshot? = null

    fun get(key: String): CatalogSnapshot? =
        if (cacheKey == key) snapshot else null

    fun put(key: String, value: CatalogSnapshot) {
        cacheKey = key
        snapshot = value
    }

    fun invalidate() {
        cacheKey = null
        snapshot = null
    }
}
