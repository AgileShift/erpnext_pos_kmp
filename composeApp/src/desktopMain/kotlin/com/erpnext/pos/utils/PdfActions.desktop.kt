package com.erpnext.pos.utils

import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun openPdfFile(path: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    return runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        } else {
            false
        }
    }.isSuccess
}

actual fun sharePdfFile(path: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    return runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file.parentFile ?: file)
        }
    }.isSuccess
}

actual suspend fun savePdfFileAs(path: String, suggestedFileName: String): String? {
    val source = File(path)
    if (!source.exists()) return null
    val chooser = JFileChooser().apply {
        dialogTitle = "Guardar factura PDF"
        selectedFile = File(suggestedFileName)
        fileFilter = FileNameExtensionFilter("PDF", "pdf")
    }
    val result = chooser.showSaveDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    val selected = chooser.selectedFile ?: return null
    val target = if (selected.name.endsWith(".pdf", ignoreCase = true)) {
        selected
    } else {
        File(selected.parentFile, "${selected.name}.pdf")
    }
    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    return target.absolutePath
}
