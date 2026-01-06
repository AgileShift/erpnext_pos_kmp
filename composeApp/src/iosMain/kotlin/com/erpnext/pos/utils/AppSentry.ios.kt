package com.erpnext.pos.utils

actual object AppSentry {
    actual fun init() = Unit
    actual fun breadcrumb(message: String) = Unit
    actual fun capture(throwable: Throwable, message: String?) = Unit
}
