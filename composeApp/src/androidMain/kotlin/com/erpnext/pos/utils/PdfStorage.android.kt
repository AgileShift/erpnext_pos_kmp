package com.erpnext.pos.utils

import android.os.Environment
import com.erpnext.pos.AppContext
import java.io.File

actual fun savePdfFile(fileName: String, bytes: ByteArray): String {
    val context = AppContext.get()
    val safeName = fileName.trim().ifBlank { "invoice.pdf" }
    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: context.filesDir
    val target = File(downloadsDir, safeName)
    target.outputStream().use { it.write(bytes) }
    return target.absolutePath
}
