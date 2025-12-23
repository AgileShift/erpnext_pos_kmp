package com.erpnext.pos.remoteSource.api

import io.ktor.client.engine.HttpClientEngine

actual fun defaultEngine(): HttpClientEngine = io.ktor.client.engine.darwin.Darwin.create()