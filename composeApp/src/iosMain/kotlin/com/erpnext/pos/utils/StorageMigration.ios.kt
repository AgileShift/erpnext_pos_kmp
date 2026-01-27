package com.erpnext.pos.utils

import platform.Foundation.NSFileManager

actual fun migratePrefsIfNeeded(legacyPath: String, newPath: String) {
    val fm = NSFileManager.defaultManager
    val legacyExists = fm.fileExistsAtPath(legacyPath)
    val targetExists = fm.fileExistsAtPath(newPath)
    if (!legacyExists || targetExists) return
    runCatching {
        fm.copyItemAtPath(legacyPath, newPath, null)
    }
}
