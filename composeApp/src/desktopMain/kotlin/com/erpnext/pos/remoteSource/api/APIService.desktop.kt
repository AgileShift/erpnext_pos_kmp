package com.erpnext.pos.remoteSource.api

import io.ktor.client.engine.HttpClientEngine

actual fun defaultEngine(): HttpClientEngine {
    return io.ktor.client.engine.cio.CIO.create()
}