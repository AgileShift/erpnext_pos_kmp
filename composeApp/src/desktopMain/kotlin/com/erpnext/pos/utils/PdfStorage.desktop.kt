package com.erpnext.pos.utils

import java.io.File

actual fun savePdfFile(fileName: String, bytes: ByteArray): String {
    val safeName = fileName.trim().ifBlank { "invoice.pdf" }
    val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
    val downloads = File(home, "Downloads")
    if (!downloads.exists()) {
        downloads.mkdirs()
    }
    val target = File(downloads, safeName)
    target.outputStream().use { it.write(bytes) }
    return target.absolutePath
}
