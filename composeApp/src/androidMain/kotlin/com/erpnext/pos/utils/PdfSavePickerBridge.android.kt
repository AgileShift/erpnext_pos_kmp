package com.erpnext.pos.utils

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.erpnext.pos.AppContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object PdfSavePickerBridge {
    private data class PendingRequest(
        val sourcePath: String,
        val continuation: CancellableContinuation<String?>
    )

    private var createDocumentLauncher: ActivityResultLauncher<String>? = null
    private var pendingRequest: PendingRequest? = null

    fun register(activity: ComponentActivity) {
        if (createDocumentLauncher != null) return
        createDocumentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            handleCreateDocumentResult(uri)
        }
    }

    fun unregister() {
        createDocumentLauncher = null
        pendingRequest?.continuation?.resume(null)
        pendingRequest = null
    }

    suspend fun saveAs(sourcePath: String, suggestedFileName: String): String? {
        val launcher = createDocumentLauncher ?: return null
        if (pendingRequest != null) return null
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return null

        return suspendCancellableCoroutine { continuation ->
            pendingRequest = PendingRequest(
                sourcePath = sourcePath,
                continuation = continuation
            )
            continuation.invokeOnCancellation {
                if (pendingRequest?.continuation == continuation) {
                    pendingRequest = null
                }
            }
            launcher.launch(ensurePdfExtension(suggestedFileName))
        }
    }

    private fun handleCreateDocumentResult(uri: Uri?) {
        val request = pendingRequest ?: return
        pendingRequest = null
        if (uri == null) {
            request.continuation.resume(null)
            return
        }
        val result = runCatching {
            copyToUri(request.sourcePath, uri)
            uri.toString()
        }.getOrNull()
        request.continuation.resume(result)
    }

    private fun copyToUri(sourcePath: String, uri: Uri) {
        val context = AppContext.get()
        val source = File(sourcePath)
        val bytes = source.readBytes()
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: error("No se pudo abrir destino para guardar PDF")
    }

    private fun ensurePdfExtension(fileName: String): String {
        val trimmed = fileName.trim().ifBlank { "invoice.pdf" }
        return if (trimmed.endsWith(".pdf", ignoreCase = true)) trimmed else "$trimmed.pdf"
    }
}
