package com.erpnext.pos.domain.utils

import platform.Foundation.NSUUID

actual class UUIDGenerator actual constructor() {
    actual fun newId(): String {
        return NSUUID.UUID().toString()
    }
}