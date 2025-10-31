package com.erpnext.pos.sync

import com.erpnext.pos.data.repositories.CheckoutRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.entities.PendingSalesInvoiceEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncManager(
    private val invoiceRepo: SalesInvoiceRepository,
    private val api: APIService,
    private val networkMonitor: Flow<Boolean>, // inject NetworkMonitor.isConnected
    private val timeProvider: TimeProvider,
    private val config: SyncConfig = SyncConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    init {
        // Observe pending count initially
        scope.launch { refreshPendingCount() }

        // Observe network -> on available trigger sync
        scope.launch {
            networkMonitor.collect { online ->
                if (online) {
                    try {
                        syncAllWithRetry()
                    } catch (_: Throwable) {
                        // syncAllWithRetry already updates state
                    } finally {
                        refreshPendingCount()
                    }
                }
            }
        }
    }

    private suspend fun refreshPendingCount() {
        val cnt = invoiceRepo . getPendingSyncInvoices ().size
        _pendingCount.value = cnt
    }

    suspend fun queueInvoice(entity: PendingSalesInvoiceEntity) {
        invoiceRepo.saveInvoiceLocally(entity)
        refreshPendingCount()
    }

    suspend fun syncAllWithRetry() {
        _syncState.value = SyncState.Syncing
        var backoff = config.initialBackoffMillis
        try {
            while (invoiceRepo.countPending() > 0) {
                try {
                    syncBatch()
                    backoff = config.initialBackoffMillis
                } catch (e: Exception) {
                    _syncState.value = SyncState.Error(e.message ?: "sync error")
                    kotlinx.coroutines.delay(backoff)
                    backoff =
                        ((backoff * config.backoffFactor).coerceAtMost(config.maxBackoffMillis.toDouble())).toLong()
                }
            }
            _syncState.value = SyncState.Idle
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "fatal sync error")
        } finally {
            refreshPendingCount()
        }
    }

    //TODO: Obtener todas las que estan synced = false
    // Convertir a DTO y enviar 1:!
    private suspend fun syncBatch() {
        /*val pending = invoiceRepo.getPendingInvoices(config.batchSize)
        if (pending.isEmpty()) return

        for (inv in pending) {
            try {
                api.sendInvoice(inv.clientId, inv.payloadJson)
                invoiceRepo.markAsSynced(inv.id)
            } catch (e: Exception) {
                invoiceRepo.markAsFailed(inv.id, timeProvider.nowMillis())
                throw e // bubble up to trigger backoff
            }
        }*/
    }
}