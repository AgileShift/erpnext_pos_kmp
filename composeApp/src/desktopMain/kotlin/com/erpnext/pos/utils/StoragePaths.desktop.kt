package com.erpnext.pos.utils

import java.io.File
import java.util.prefs.Preferences
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.erpnext.pos.utils.migratePrefsIfNeeded

actual fun prefsPath(): String {
    val dir = File(System.getProperty("user.home"), ".erpnext-pos")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val legacy = File(dir, "prefs.preferences_pb").absolutePath
    val currentSite = Preferences.userRoot()
        .node("com.erpnext.pos.secure_prefs_v2")
        .get("current_site", null)
    return if (currentSite.isNullOrBlank()) {
        legacy
    } else {
        val instanceKey = instanceKeyFromUrl(currentSite)
        val target = File(dir, "prefs_$instanceKey.preferences_pb").absolutePath
        migratePrefsIfNeeded(legacy, target)
        target
    }
}
