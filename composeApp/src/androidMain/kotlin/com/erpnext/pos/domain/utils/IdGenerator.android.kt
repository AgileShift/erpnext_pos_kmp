package com.erpnext.pos.domain.utils

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual class UUIDGenerator actual constructor() {
    @OptIn(ExperimentalUuidApi::class)
    actual fun newId(): String {
        return Uuid.random().toString()
    }
}