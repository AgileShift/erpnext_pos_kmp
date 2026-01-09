package com.erpnext.pos.sync

import com.erpnext.pos.base.Resource
import com.erpnext.pos.data.repositories.CompanyRepository
import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.data.repositories.DeliveryChargesRepository
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.data.repositories.ModeOfPaymentRepository
import com.erpnext.pos.data.repositories.PaymentTermsRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.sync.PushSyncManager
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.auth.SessionRefresher
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ISyncManager {
    val state: StateFlow<SyncState>
    fun fullSync(ttlHours: Int = SyncTTL.DEFAULT_TTL_HOURS)
}

class SyncManager(
    private val invoiceRepo: SalesInvoiceRepository,
    private val customerRepo: CustomerRepository,
    private val inventoryRepo: InventoryRepository,
    private val modeOfPaymentRepo: ModeOfPaymentRepository,
    private val paymentTermsRepo: PaymentTermsRepository,
    private val deliveryChargesRepo: DeliveryChargesRepository,
    private val exchangeRateRepo: ExchangeRateRepository,
    private val syncPreferences: SyncPreferences,
    private val companyInfoRepo: CompanyRepository,
    private val cashBoxManager: CashBoxManager,
    private val networkMonitor: NetworkMonitor,
    private val sessionRefresher: SessionRefresher,
    private val syncContextProvider: SyncContextProvider,
    private val pushSyncManager: PushSyncManager
) : ISyncManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncSettingsCache =
        SyncSettings(autoSync = true, syncOnStartup = true, wifiOnly = false, lastSyncAt = null)

    private val _state = MutableStateFlow<SyncState>(SyncState.IDLE)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        observeSyncSettings()
        observeConnectivity()
    }

    @OptIn(ExperimentalTime::class)
    override fun fullSync(ttlHours: Int) {
        if (_state.value is SyncState.SYNCING) return

        scope.launch {
            AppSentry.breadcrumb("SyncManager.fullSync start (ttl=$ttlHours)")
            AppLogger.info("SyncManager.fullSync start (ttl=$ttlHours)")
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                _state.value = SyncState.ERROR("No hay conexión a internet.")
                AppSentry.breadcrumb("SyncManager.fullSync aborted: offline")
                AppLogger.warn("SyncManager.fullSync aborted: offline")
                return@launch
            }

            if (!sessionRefresher.ensureValidSession()) {
                _state.value = SyncState.ERROR("Sesión inválida. Redirigiendo al login.")
                AppSentry.breadcrumb("SyncManager.fullSync aborted: invalid session")
                AppLogger.warn("SyncManager.fullSync aborted: invalid session")
                return@launch
            }

            try {
                coroutineScope {
                    val jobs = listOf(async {
                        _state.value = SyncState.SYNCING("Metodos de pago...")
                        AppSentry.breadcrumb("Sync: mode of payment")
                        val result =
                            modeOfPaymentRepo.sync(ttlHours).filter { it !is Resource.Loading }
                                .first()
                        if (result is Resource.Error) {
                            val error = Exception(
                                result.message ?: "Error al sincronizar métodos de pago"
                            )
                            AppSentry.capture(error, "Sync: mode of payment failed")
                            AppLogger.warn("Sync: mode of payment failed", error)
                            throw error
                        }
                    }, async {
                        _state.value = SyncState.SYNCING("Clientes...")

                        AppSentry.breadcrumb("Sync: customers")
                        val result = customerRepo.sync().filter { it !is Resource.Loading }.first()
                        if (result is Resource.Error) {
                            val error = Exception(result.message ?: "Error al sincronizar clientes")
                            AppSentry.capture(error, "Sync: customers failed")
                            AppLogger.warn("Sync: customers failed", error)
                            throw error
                        }
                    }, async {
                        _state.value = SyncState.SYNCING("Categorias de Productos...")

                        AppSentry.breadcrumb("Sync: categories")
                        val result =
                            inventoryRepo.getCategories().filter { it !is Resource.Loading }.first()
                        if (result is Resource.Error) {
                            val error =
                                Exception(result.message ?: "Error al sincronizar categorías")
                            AppSentry.capture(error, "Sync: categories failed")
                            AppLogger.warn("Sync: categories failed", error)
                            throw error
                        }
                    }, async {
                        _state.value = SyncState.SYNCING("Inventario...")
                        AppSentry.breadcrumb("Sync: inventory")
                        val result = inventoryRepo.sync().filter { it !is Resource.Loading }.first()
                        if (result is Resource.Error) {
                            val error =
                                Exception(result.message ?: "Error al sincronizar inventario")
                            AppSentry.capture(error, "Sync: inventory failed")
                            AppLogger.warn("Sync: inventory failed", error)
                            throw error
                        }
                    }, async {
                        _state.value = SyncState.SYNCING("Facturas...")
                        AppSentry.breadcrumb("Sync: invoices")
                        val result = invoiceRepo.sync().filter { it !is Resource.Loading }.first()
                        if (result is Resource.Error) {
                            val error = Exception(result.message ?: "Error al sincronizar facturas")
                            AppSentry.capture(error, "Sync: invoices failed")
                            AppLogger.warn("Sync: invoices failed", error)
                            throw error
                        }
                    }, async {
                        _state.value = SyncState.SYNCING("Términos de pago...")
                        AppSentry.breadcrumb("Sync: payment terms")
                        runCatching { paymentTermsRepo.fetchPaymentTerms() }.getOrElse { error ->
                                val exception = Exception(
                                    error.message ?: "Error al sincronizar términos de pago"
                                )
                                AppSentry.capture(exception, "Sync: payment terms failed")
                                AppLogger.warn("Sync: payment terms failed", exception)
                                throw exception
                            }
                    }, async {
                        _state.value = SyncState.SYNCING("Cargos de envío...")
                        AppSentry.breadcrumb("Sync: delivery charges")
                        runCatching { deliveryChargesRepo.fetchDeliveryCharges() }.getOrElse { error ->
                                val exception = Exception(
                                    error.message ?: "Error al sincronizar cargos de envío"
                                )
                                AppSentry.capture(exception, "Sync: delivery charges failed")
                                AppLogger.warn(
                                    "Sync: delivery charges failed", exception
                                )
                                throw exception
                            }
                    }, async {
                        _state.value = SyncState.SYNCING("Informacion de la empresa...")
                        AppSentry.breadcrumb("Sync: company info")
                        runCatching {
                            companyInfoRepo.getCompanyInfo()
                        }.getOrElse { error ->
                            AppSentry.capture(
                                error, "Sync: company info failed"
                            )
                            AppLogger.warn(
                                "Sync: company info failed", error
                            )
                        }
                    }, async {
                        _state.value = SyncState.SYNCING("Tasas de cambio...")
                        AppSentry.breadcrumb("Sync: exchange rates")
                        val context = cashBoxManager.getContext()
                        val currencies = mutableSetOf<String>()
                        context?.let {
                            currencies.add(it.currency)
                            it.allowedCurrencies.mapTo(currencies) { option -> option.code }
                        }
                        currencies.filter { it.isNotBlank() }.map { it.uppercase() }.distinct()
                            .forEach { currency ->
                                runCatching {
                                    exchangeRateRepo.getRate("USD", currency)
                                }.getOrElse { error ->
                                    AppSentry.capture(
                                        error, "Sync: exchange rate failed for $currency"
                                    )
                                    AppLogger.warn(
                                        "Sync: exchange rate failed for $currency", error
                                    )
                                }
                            }
                    })
                    jobs.awaitAll()
                    runPushQueue()
                }
                _state.value = SyncState.SUCCESS
                AppSentry.breadcrumb("SyncManager.fullSync success")
                AppLogger.info("SyncManager.fullSync success")
                syncPreferences.setLastSyncAt(Clock.System.now().toEpochMilliseconds())
            } catch (e: Exception) {
                e.printStackTrace()
                AppSentry.capture(e, "SyncManager.fullSync failed")
                AppLogger.warn("SyncManager.fullSync failed", e)
                _state.value = SyncState.ERROR("Error durante la sincronización: ${e.message}")
            } finally {
                delay(5000)
                _state.value = SyncState.IDLE
            }
        }
    }

    private suspend fun runPushQueue() {
        val ctx = syncContextProvider.buildContext()
        if (ctx == null) {
            AppLogger.warn("SyncManager: push queue skipped because context is not ready")
            return
        }
        _state.value = SyncState.SYNCING("Sincronizando push...")
        try {
            pushSyncManager.runPushQueue(ctx) { docType ->
                _state.value = SyncState.SYNCING("Push: $docType")
            }
        } catch (e: Throwable) {
            AppSentry.capture(e, "SyncManager.pushQueue failed")
            AppLogger.warn("SyncManager.pushQueue failed", e)
            throw e
        }
    }

    private fun observeSyncSettings() {
        scope.launch {
            syncPreferences.settings.collect { settings ->
                syncSettingsCache = settings
            }
        }
    }

    private fun observeConnectivity() {
        scope.launch {
            var wasConnected = false
            networkMonitor.isConnected.collect { connected ->
                if (connected && !wasConnected && shouldAutoSyncOnConnection()) {
                    fullSync()
                }
                wasConnected = connected
            }
        }
    }

    private fun shouldAutoSyncOnConnection(): Boolean {
        if (!syncSettingsCache.autoSync) return false
        if (syncSettingsCache.wifiOnly) return false
        return _state.value !is SyncState.SYNCING
    }
}
