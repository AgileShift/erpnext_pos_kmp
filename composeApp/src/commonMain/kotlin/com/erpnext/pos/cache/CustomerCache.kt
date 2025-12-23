package com.erpnext.pos.cache

import com.erpnext.pos.remoteSource.dto.v2.CustomerSnapshot

class CustomerCache {
    private var key: String? = null
    private var data: List<CustomerSnapshot>? = null

    fun get(key: String): List<CustomerSnapshot>? =
        if (this.key == key) data else null

    fun put(key: String, value: List<CustomerSnapshot>) {
        this.key = key
        this.data = value
    }

    fun invalidate() {
        key = null
        data = null
    }
}
