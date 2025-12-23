package com.erpnext.pos.utils

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual class TimeProvider actual constructor() {
    actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}