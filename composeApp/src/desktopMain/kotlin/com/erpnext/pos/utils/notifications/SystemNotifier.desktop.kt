package com.erpnext.pos.utils.notifications

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

actual fun notifySystem(title: String, message: String) {
    if (!SystemTray.isSupported()) return
    val tray = SystemTray.getSystemTray()
    val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val trayIcon = TrayIcon(image, "ERPNext POS")
    trayIcon.isImageAutoSize = true
    try {
        tray.add(trayIcon)
        trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO)
    } catch (_: Throwable) {
        // Ignore notification failures on desktop
    } finally {
        try {
            tray.remove(trayIcon)
        } catch (_: Throwable) {
        }
    }
}

actual fun scheduleDailyInventoryReminder(
    enabled: Boolean,
    title: String,
    message: String,
    hour: Int,
    minute: Int
) {
    // Desktop reminders are handled while the app is running.
}

actual fun configureInventoryAlertWorker(enabled: Boolean, hour: Int, minute: Int) {
    // Desktop alerts are checked while the app is running.
}
