package com.erpnext.pos.utils

actual object AppSentry {
    actual fun init() = Unit
    actual fun breadcrumb(message: String, category: String?, data: Map<String, String>) = Unit
    actual fun capture(
        throwable: Throwable,
        message: String?,
        tags: Map<String, String>,
        extras: Map<String, String>
    ) = Unit
}
