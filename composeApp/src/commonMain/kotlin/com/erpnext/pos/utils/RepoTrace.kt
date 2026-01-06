package com.erpnext.pos.utils

object RepoTrace {
    fun breadcrumb(repo: String, action: String, detail: String? = null) {
        val msg = if (detail.isNullOrBlank()) "$repo: $action" else "$repo: $action | $detail"
        AppSentry.breadcrumb(msg)
    }

    fun capture(repo: String, action: String, throwable: Throwable) {
        AppSentry.capture(throwable, "$repo: $action failed")
    }
}
