package com.erpnext.pos.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual fun openPdfFile(path: String): Boolean {
    val fileUrl = NSURL.fileURLWithPath(path)
    return runCatching {
        UIApplication.sharedApplication.openURL(fileUrl)
    }.getOrDefault(false)
}

actual fun sharePdfFile(path: String): Boolean {
    val presenter = currentPresenter() ?: return false
    val fileUrl = NSURL.fileURLWithPath(path)
    return runCatching {
        val sheet = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null
        )
        presenter.presentViewController(sheet, animated = true, completion = null)
        true
    }.getOrDefault(false)
}

actual suspend fun savePdfFileAs(path: String, suggestedFileName: String): String? {
    val presenter = currentPresenter() ?: return null
    val fileUrl = NSURL.fileURLWithPath(path)
    if (!fileUrl.isFileURL()) return null

    return suspendCancellableCoroutine { continuation ->
        val picker = UIDocumentPickerViewController(
            forExportingURLs = listOf(fileUrl),
            asCopy = true
        )

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val picked = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                continuation.resume(picked?.path)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                continuation.resume(null)
            }
        }

        picker.setDelegate(delegate)
        IosPickerDelegateHolder.delegate = delegate

        continuation.invokeOnCancellation {
            IosPickerDelegateHolder.delegate = null
        }

        presenter.presentViewController(picker, animated = true, completion = null)
    }
}

private object IosPickerDelegateHolder {
    var delegate: UIDocumentPickerDelegateProtocol? = null
}

private fun currentPresenter(): UIViewController? {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    var top: UIViewController = root
    while (top.presentedViewController != null) {
        top = top.presentedViewController!!
    }
    return top
}
