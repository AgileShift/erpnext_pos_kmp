package com.erpnext.pos.utils

import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDefaults
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.erpnext.pos.utils.migratePrefsIfNeeded

actual fun prefsPath(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val dir = urls.firstOrNull()
    val base = dir?.path ?: ""
    val legacy = "$base/prefs.preferences_pb"
    val currentSite = NSUserDefaults.standardUserDefaults.stringForKey("current_site")
    return if (currentSite.isNullOrBlank()) {
        legacy
    } else {
        val instanceKey = instanceKeyFromUrl(currentSite)
        val target = "$base/prefs_$instanceKey.preferences_pb"
        migratePrefsIfNeeded(legacy, target)
        target
    }
}
