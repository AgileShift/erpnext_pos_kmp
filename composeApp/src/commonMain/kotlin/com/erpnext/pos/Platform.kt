package com.erpnext.pos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform