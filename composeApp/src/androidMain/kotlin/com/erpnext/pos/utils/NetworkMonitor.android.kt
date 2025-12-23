package com.erpnext.pos.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

actual class NetworkMonitor(private val context: Context) {
    actual val isConnected = callbackFlow {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true).isSuccess
            }

            override fun onLost(network: Network) {
                trySend(false).isSuccess
            }
        }
        val request = NetworkRequest.Builder().build()
        manager.registerNetworkCallback(request, callback)

        // Emitir estado inicial
        trySend(manager.activeNetwork != null).isSuccess
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}