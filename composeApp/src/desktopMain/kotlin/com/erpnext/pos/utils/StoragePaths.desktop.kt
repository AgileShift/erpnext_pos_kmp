package com.erpnext.pos.utils

import java.io.File

actual fun prefsPath(): String {
    val dir = File(System.getProperty("user.home"), ".erpnext-pos")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File(dir, "prefs.preferences_pb").absolutePath
}
