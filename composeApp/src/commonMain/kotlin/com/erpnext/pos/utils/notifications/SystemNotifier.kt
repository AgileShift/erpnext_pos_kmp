package com.erpnext.pos.utils.notifications

expect fun notifySystem(title: String, message: String)

expect fun scheduleDailyInventoryReminder(
    enabled: Boolean,
    title: String,
    message: String,
    hour: Int,
    minute: Int
)

expect fun configureInventoryAlertWorker(
    enabled: Boolean,
    hour: Int,
    minute: Int
)
