package com.erpnext.pos.domain.policy

interface CatalogInvalidationPolicy {
    fun invalidate()
}