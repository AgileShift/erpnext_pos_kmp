package com.erpnext.pos.utils

expect object AppSentry {
    fun init()
    fun breadcrumb(
        message: String,
        category: String? = null,
        data: Map<String, String> = emptyMap()
    )
    fun capture(
        throwable: Throwable,
        message: String? = null,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, String> = emptyMap()
    )
}
