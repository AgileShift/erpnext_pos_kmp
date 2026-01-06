package com.erpnext.pos.utils

expect object AppSentry {
    fun init()
    fun breadcrumb(message: String)
    fun capture(throwable: Throwable, message: String? = null)
}
