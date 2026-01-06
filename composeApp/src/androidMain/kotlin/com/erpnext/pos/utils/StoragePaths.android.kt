package com.erpnext.pos.utils

import com.erpnext.pos.AppContext
import java.io.File

actual fun prefsPath(): String {
    val dir = AppContext.get().filesDir
    return File(dir, "prefs.preferences_pb").absolutePath
}
