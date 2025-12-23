package com.erpnext.pos.domain.policy

import com.erpnext.pos.cache.CatalogCache

class DefaultCatalogInvalidationPolicy(
    private val cache: CatalogCache
) : CatalogInvalidationPolicy {
    override fun invalidate() {
        cache.invalidate()
    }
}