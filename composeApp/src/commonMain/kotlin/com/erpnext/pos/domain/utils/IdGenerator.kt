package com.erpnext.pos.domain.utils

expect class UUIDGenerator() {
    fun newId(): String
}