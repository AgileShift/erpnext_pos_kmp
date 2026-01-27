package com.erpnext.pos.utils

import com.erpnext.pos.AppContext
import com.erpnext.pos.AndroidTokenStore
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.erpnext.pos.utils.migratePrefsIfNeeded
import java.io.File
import kotlinx.coroutines.runBlocking

actual fun prefsPath(): String {
    val dir = AppContext.get().filesDir
    val legacy = File(dir, "prefs.preferences_pb").absolutePath
    val currentSite = runBlocking { AndroidTokenStore(AppContext.get()).getCurrentSite() }
    return if (currentSite.isNullOrBlank()) {
        legacy
    } else {
        val instanceKey = instanceKeyFromUrl(currentSite)
        val target = File(dir, "prefs_$instanceKey.preferences_pb").absolutePath
        migratePrefsIfNeeded(legacy, target)
        target
    }
}
