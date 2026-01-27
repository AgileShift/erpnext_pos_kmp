package com.erpnext.pos.utils

import java.io.File

actual fun migratePrefsIfNeeded(legacyPath: String, newPath: String) {
    val legacy = File(legacyPath)
    val target = File(newPath)
    if (!legacy.exists() || target.exists()) return
    runCatching {
        target.parentFile?.mkdirs()
        legacy.copyTo(target, overwrite = false)
    }
}
