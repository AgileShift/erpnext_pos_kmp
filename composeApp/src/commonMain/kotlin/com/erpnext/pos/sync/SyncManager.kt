package com.erpnext.pos.sync

import com.erpnext.pos.base.Resource
import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface ISyncManager {
    val state: StateFlow<SyncState>
    fun fullSync()
}

class SyncManager(
    private val invoiceRepo: SalesInvoiceRepository,
    private val customerRepo: CustomerRepository,
    private val inventoryRepo: InventoryRepository,
    private val networkMonitor: NetworkMonitor,
) : ISyncManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow<SyncState>(SyncState.IDLE)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    override fun fullSync() {
        if (_state.value is SyncState.SYNCING) return

        scope.launch {
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                _state.value = SyncState.ERROR("No hay conexión a internet.")
                return@launch
            }

            try {
                coroutineScope {
                    val jobs = listOf(
                        async {
                            _state.value = SyncState.SYNCING("Clientes...")

                            customerRepo.sync()
                                .filter { it !is Resource.Loading }
                                .first()
                        },
                        async {
                            _state.value =
                                SyncState.SYNCING("Categorias de Productos...")

                            inventoryRepo.getCategories()
                                .filter { it !is Resource.Loading }
                                .first()
                        },
                        async {
                            _state.value = SyncState.SYNCING("Inventario...")

                            inventoryRepo.sync()
                                .filter { it !is Resource.Loading }
                                .first()
                        },
                        async {
                            _state.value = SyncState.SYNCING("Facturas...")
                            invoiceRepo.sync()
                                .filter { it !is Resource.Loading }
                                .first()
                        }
                    )
                    jobs.awaitAll()
                }
                _state.value = SyncState.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = SyncState.ERROR("Error durante la sincronización: ${e.message}")
            } finally {
                delay(5000)
                _state.value = SyncState.IDLE
            }
        }
    }
}
