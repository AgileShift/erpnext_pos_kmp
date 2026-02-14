package com.erpnext.pos.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.writeToFile

actual fun savePdfFile(fileName: String, bytes: ByteArray): String {
    val safeName = fileName.trim().ifBlank { "invoice.pdf" }
    val directory = NSTemporaryDirectory()
    val target = "$directory/$safeName"
    val data = bytes.toNSData()
    data.writeToFile(target, atomically = true)
    return target
}

private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
