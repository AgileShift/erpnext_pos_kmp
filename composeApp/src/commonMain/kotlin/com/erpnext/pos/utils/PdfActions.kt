package com.erpnext.pos.utils

expect fun openPdfFile(path: String): Boolean

expect fun sharePdfFile(path: String): Boolean

expect suspend fun savePdfFileAs(path: String, suggestedFileName: String): String?
