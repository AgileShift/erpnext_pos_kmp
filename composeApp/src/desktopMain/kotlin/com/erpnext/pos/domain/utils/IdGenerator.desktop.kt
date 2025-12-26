package com.erpnext.pos.domain.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
actual class UUIDGenerator actual constructor() {
    actual fun newId(): String {
        return Uuid.random().toString()
    }
}