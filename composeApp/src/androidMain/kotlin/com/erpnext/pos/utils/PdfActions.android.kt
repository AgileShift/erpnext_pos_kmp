package com.erpnext.pos.utils

import android.content.Intent
import androidx.core.content.FileProvider
import com.erpnext.pos.AppContext
import java.io.File

private const val PDF_MIME = "application/pdf"

actual fun openPdfFile(path: String): Boolean {
    val context = AppContext.get()
    val file = File(path)
    if (!file.exists()) return false
    return runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, PDF_MIME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

actual fun sharePdfFile(path: String): Boolean {
    val context = AppContext.get()
    val file = File(path)
    if (!file.exists()) return false
    return runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = PDF_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir factura PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    }.getOrDefault(false)
}

actual suspend fun savePdfFileAs(path: String, suggestedFileName: String): String? {
    return PdfSavePickerBridge.saveAs(path, suggestedFileName)
}
