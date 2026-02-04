package com.erpnext.pos.utils.notifications

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.Foundation.NSDateComponents

actual fun notifySystem(title: String, message: String) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.requestAuthorizationWithOptions(
        options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
    ) { _, _ -> }

    val content = UNMutableNotificationContent().apply {
        this.title = title
        this.body = message
    }
    val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = "inventory_alerts",
        content = content,
        trigger = trigger
    )
    center.addNotificationRequest(request, withCompletionHandler = null)
}

actual fun scheduleDailyInventoryReminder(
    enabled: Boolean,
    title: String,
    message: String,
    hour: Int,
    minute: Int
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val identifier = "inventory_alerts_daily"
    if (!enabled) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
        return
    }
    center.requestAuthorizationWithOptions(
        options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
    ) { _, _ -> }

    val content = UNMutableNotificationContent().apply {
        this.title = title
        this.body = message
    }
    val components = NSDateComponents().apply {
        this.hour = hour
        this.minute = minute
    }
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
        dateComponents = components,
        repeats = true
    )
    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = identifier,
        content = content,
        trigger = trigger
    )
    center.addNotificationRequest(request, withCompletionHandler = null)
}

actual fun configureInventoryAlertWorker(enabled: Boolean, hour: Int, minute: Int) {
    // iOS uses scheduled local notifications; background workers are not configured here.
}
