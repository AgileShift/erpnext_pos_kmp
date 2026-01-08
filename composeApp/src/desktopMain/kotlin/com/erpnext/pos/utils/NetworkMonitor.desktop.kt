package com.erpnext.pos.utils

import io.ktor.network.sockets.SocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

suspend fun hasInternetByTcp(
    host: String = "1.1.1.1",
    port: Int = 53,
    timeoutMs: Int = 1500
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
        true
    }.getOrElse { false }
}

actual class NetworkMonitor {

    private val intervalMs = 5_000L

    private suspend fun checkConnected(): Boolean {
        return hasInternetByTcp()
    }

    actual val isConnected: Flow<Boolean> =
        flow {
            emit(checkConnected())
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                emit(checkConnected())
            }
        }.flowOn(Dispatchers.IO)
            .distinctUntilChanged()
            .conflate()
}