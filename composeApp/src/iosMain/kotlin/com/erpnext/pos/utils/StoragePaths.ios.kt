package com.erpnext.pos.utils

import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDocumentDirectory

actual fun prefsPath(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val dir = urls.firstOrNull()
    val base = dir?.path ?: ""
    return "$base/prefs.preferences_pb"
}
