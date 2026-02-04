package com.erpnext.pos.sync

import com.erpnext.pos.base.Resource
import com.erpnext.pos.data.repositories.CompanyRepository
import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.data.repositories.CustomerGroupRepository
import com.erpnext.pos.data.repositories.DeliveryChargesRepository
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.data.repositories.ModeOfPaymentRepository
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodSyncRepository
import com.erpnext.pos.data.repositories.PaymentTermsRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.ContactRepository
import com.erpnext.pos.data.repositories.AddressRepository
import com.erpnext.pos.data.repositories.TerritoryRepository
import com.erpnext.pos.sync.PushSyncRunner
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.domain.policy.DefaultPolicy
import com.erpnext.pos.domain.policy.PolicyInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    fun fullSync(ttlHours: Int = SyncTTL.DEFAULT_TTL_HOURS, force: Boolean = false)
    fun syncInventory(force: Boolean = false)
}

class SyncManager(
    private val invoiceRepo: SalesInvoiceRepository,
    private val customerRepo: CustomerRepository,
    private val inventoryRepo: InventoryRepository,
    private val modeOfPaymentRepo: ModeOfPaymentRepository,
    private val posProfilePaymentMethodSyncRepository: PosProfilePaymentMethodSyncRepository,
    private val stockSettingsRepository: com.erpnext.pos.data.repositories.StockSettingsRepository,
    private val paymentTermsRepo: PaymentTermsRepository,
    private val deliveryChargesRepo: DeliveryChargesRepository,
    private val contactRepo: ContactRepository,
    private val addressRepo: AddressRepository,
    private val customerGroupRepo: CustomerGroupRepository,
    private val territoryRepo: TerritoryRepository,
    private val exchangeRateRepo: ExchangeRateRepository,
    private val syncPreferences: SyncPreferences,
    private val companyInfoRepo: CompanyRepository,
    private val cashBoxManager: CashBoxManager,
    private val posProfileDao: POSProfileDao,
    private val networkMonitor: NetworkMonitor,
    private val sessionRefresher: SessionRefresher,
    private val syncContextProvider: SyncContextProvider,
    private val pushSyncManager: PushSyncRunner
) : ISyncManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncSettingsCache =
        SyncSettings(
            autoSync = true,
            syncOnStartup = true,
            wifiOnly = false,
            lastSyncAt = null,
            useTtl = false
        )
    private var lastSyncAttemptAt: Long? = null
    private val minSyncIntervalMillis = 2 * 60 * 1000L

    private val _state = MutableStateFlow<SyncState>(SyncState.IDLE)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        observeSyncSettings()
        observeConnectivity()
    }

    @OptIn(ExperimentalTime::class)
    override fun fullSync(ttlHours: Int, force: Boolean) {
        if (_state.value is SyncState.SYNCING) return

        if (!cashBoxManager.cashboxState.value) {
            AppLogger.info("SyncManager.fullSync skipped: cashbox is closed")
            _state.value = SyncState.IDLE
            return
        }
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force) {
            val lastAttempt = lastSyncAttemptAt
            if (lastAttempt != null && now - lastAttempt < minSyncIntervalMillis) {
                AppLogger.info("SyncManager.fullSync skipped: within min interval")
                return
            }
            if (syncSettingsCache.useTtl &&
                !SyncTTL.isExpired(syncSettingsCache.lastSyncAt, ttlHours)
            ) {
                AppLogger.info("SyncManager.fullSync skipped: TTL not expired")
                return
            }
        }
        lastSyncAttemptAt = now

        /* TODO: Agregar POS Profiles con sus detalles, esto lo vamos a remover en la v2
            Porque los perfiles ya vendran en el initial data y solo los que pertenecen al usuario
         */
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

            val steps = buildFullSyncSteps(ttlHours)
            LoadingIndicator.start(
                message = "Sincronizando datos...",
                progress = 0f,
                currentStep = 0,
                totalSteps = steps.size
            )
            try {
                steps.forEachIndexed { index, step ->
                    val currentStep = index + 1
                    updateSyncProgress(
                        message = step.message,
                        currentStep = currentStep,
                        totalSteps = steps.size
                    )
                    step.action()
                    LoadingIndicator.update(progress = currentStep.toFloat() / steps.size.toFloat())
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
                LoadingIndicator.stop()
                delay(5000)
                _state.value = SyncState.IDLE
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun syncInventory(force: Boolean) {
        if (_state.value is SyncState.SYNCING) return
        if (!cashBoxManager.cashboxState.value) {
            AppLogger.info("SyncManager.syncInventory skipped: cashbox is closed")
            return
        }
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force) {
            val lastAttempt = lastSyncAttemptAt
            if (lastAttempt != null && now - lastAttempt < minSyncIntervalMillis) {
                AppLogger.info("SyncManager.syncInventory skipped: within min interval")
                return
            }
        }
        lastSyncAttemptAt = now

        scope.launch {
            AppSentry.breadcrumb("SyncManager.syncInventory start")
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                AppLogger.warn("SyncManager.syncInventory aborted: offline")
                return@launch
            }
            if (!sessionRefresher.ensureValidSession()) {
                AppLogger.warn("SyncManager.syncInventory aborted: invalid session")
                return@launch
            }
            LoadingIndicator.start(
                message = "Sincronizando inventario...",
                progress = 0f,
                currentStep = 1,
                totalSteps = 1
            )
            try {
                updateSyncProgress(
                    message = "Sincronizando inventario...",
                    currentStep = 1,
                    totalSteps = 1
                )
                val result = inventoryRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(
                        result.message ?: "Error al sincronizar inventario"
                    )
                    AppSentry.capture(error, "Sync: inventory only failed")
                    AppLogger.warn("Sync: inventory only failed", error)
                }
                LoadingIndicator.update(progress = 1f)
                _state.value = SyncState.SUCCESS
            } catch (e: Exception) {
                AppSentry.capture(e, "SyncManager.syncInventory failed")
                AppLogger.warn("SyncManager.syncInventory failed", e)
                _state.value = SyncState.ERROR("Error al sincronizar inventario: ${e.message}")
            } finally {
                LoadingIndicator.stop()
                delay(2000)
                _state.value = SyncState.IDLE
            }
        }
    }

    private suspend fun runPushQueue() {
        if (!cashBoxManager.cashboxState.value) {
            AppLogger.warn("SyncManager: push queue skipped because cashbox is closed")
            return
        }
        val ctx = syncContextProvider.buildContext() ?: run {
            val base = cashBoxManager.getContext()
            if (base == null) {
                AppLogger.warn("SyncManager: push queue skipped because context is not ready")
                return
            }
            val instanceId = base.company.ifBlank { base.profileName }.ifBlank { base.username }
            val companyId = base.company.ifBlank { base.profileName }
            if (instanceId.isBlank() || companyId.isBlank()) {
                AppLogger.warn("SyncManager: push queue skipped because instance/company is blank")
                return
            }
            SyncContext(
                instanceId = instanceId,
                companyId = companyId,
                territoryId = base.territory ?: base.route ?: "",
                warehouseId = base.warehouse ?: "",
                priceList = base.priceList ?: base.currency,
                fromDate = syncContextProvider.buildContext()?.fromDate
                    ?: DefaultPolicy(PolicyInput(3)).invoicesFromDate()
            )
        }
        val syncing = _state.value as? SyncState.SYNCING
        val currentStep = syncing?.currentStep ?: 1
        val totalSteps = syncing?.totalSteps ?: 1
        updateSyncProgress("Sincronizando pendientes...", currentStep, totalSteps)
        try {
            pushSyncManager.runPushQueue(ctx) { docType ->
                updateSyncProgressAsync("Sincronizando $docType...", currentStep, totalSteps)
            }
        } catch (e: Throwable) {
            AppSentry.capture(e, "SyncManager.pushQueue failed")
            AppLogger.warn("SyncManager.pushQueue failed", e)
            throw e
        }
    }

    private suspend fun updateSyncProgress(message: String, currentStep: Int, totalSteps: Int) {
        _state.value = SyncState.SYNCING(
            message = message,
            currentStep = currentStep,
            totalSteps = totalSteps
        )
        LoadingIndicator.update(
            message = "$message ($currentStep/$totalSteps)",
            progress = currentStep.toFloat() / totalSteps.toFloat(),
            currentStep = currentStep,
            totalSteps = totalSteps
        )
    }

    private fun updateSyncProgressAsync(message: String, currentStep: Int, totalSteps: Int) {
        _state.value = SyncState.SYNCING(
            message = message,
            currentStep = currentStep,
            totalSteps = totalSteps
        )
        scope.launch {
            LoadingIndicator.update(
                message = "$message ($currentStep/$totalSteps)",
                progress = currentStep.toFloat() / totalSteps.toFloat(),
                currentStep = currentStep,
                totalSteps = totalSteps
            )
        }
    }

    private fun buildFullSyncSteps(ttlHours: Int): List<SyncStep> {
        return listOf(
            SyncStep("Sincronizando métodos de pago...") {
                AppSentry.breadcrumb("Sync: mode of payment")
                val result = modeOfPaymentRepo.sync(ttlHours).filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar métodos de pago")
                    AppSentry.capture(error, "Sync: mode of payment failed")
                    AppLogger.warn("Sync: mode of payment failed", error)
                    throw error
                }
            },
            SyncStep("Sincronizando clientes...") {
                AppSentry.breadcrumb("Sync: customers")
                val result = customerRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar clientes")
                    AppSentry.capture(error, "Sync: customers failed")
                    AppLogger.warn("Sync: customers failed", error)
                    throw error
                }
            },
            SyncStep("Sincronizando categorías de productos...") {
                AppSentry.breadcrumb("Sync: categories")
                val result = inventoryRepo.getCategories().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar categorías")
                    AppSentry.capture(error, "Sync: categories failed")
                    AppLogger.warn("Sync: categories failed", error)
                    throw error
                }
            },
            SyncStep("Sincronizando inventario...") {
                AppSentry.breadcrumb("Sync: inventory")
                val result = inventoryRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar inventario")
                    AppSentry.capture(error, "Sync: inventory failed")
                    AppLogger.warn("Sync: inventory failed", error)
                    throw error
                }
            },
            SyncStep("Sincronizando facturas...") {
                AppSentry.breadcrumb("Sync: invoices")
                val result = invoiceRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar facturas")
                    AppSentry.capture(error, "Sync: invoices failed")
                    AppLogger.warn("Sync: invoices failed", error)
                    throw error
                }
            },
            SyncStep("Sincronizando términos de pago...") {
                AppSentry.breadcrumb("Sync: payment terms")
                runCatching { paymentTermsRepo.fetchPaymentTerms() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar términos de pago")
                    AppSentry.capture(exception, "Sync: payment terms failed")
                    AppLogger.warn("Sync: payment terms failed", exception)
                    throw exception
                }
            },
            SyncStep("Sincronizando grupos de cliente...") {
                AppSentry.breadcrumb("Sync: customer groups")
                runCatching { customerGroupRepo.fetchCustomerGroups() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar grupos de cliente")
                    AppSentry.capture(exception, "Sync: customer groups failed")
                    AppLogger.warn("Sync: customer groups failed", exception)
                    throw exception
                }
            },
            SyncStep("Sincronizando territorios...") {
                AppSentry.breadcrumb("Sync: territories")
                runCatching { territoryRepo.fetchTerritories() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar territorios")
                    AppSentry.capture(exception, "Sync: territories failed")
                    AppLogger.warn("Sync: territories failed", exception)
                    throw exception
                }
            },
            SyncStep("Sincronizando cargos de envío...") {
                AppSentry.breadcrumb("Sync: delivery charges")
                runCatching { deliveryChargesRepo.fetchDeliveryCharges() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar cargos de envío")
                    AppSentry.capture(exception, "Sync: delivery charges failed")
                    AppLogger.warn("Sync: delivery charges failed", exception)
                    throw exception
                }
            },
            SyncStep("Sincronizando contactos...") {
                AppSentry.breadcrumb("Sync: contacts")
                runCatching { contactRepo.fetchCustomerContacts() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar contactos")
                    AppSentry.capture(exception, "Sync: contacts failed")
                    AppLogger.warn("Sync: contacts failed", exception)
                    throw exception
                }
            },
            SyncStep("Sincronizando direcciones...") {
                AppSentry.breadcrumb("Sync: addresses")
                runCatching { addressRepo.fetchCustomerAddresses() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar direcciones")
                    AppSentry.capture(exception, "Sync: addresses failed")
                    AppLogger.warn("Sync: addresses failed", exception)
                    throw exception
                }
            },
            SyncStep("Sincronizando información de la empresa...") {
                AppSentry.breadcrumb("Sync: company info")
                runCatching { companyInfoRepo.getCompanyInfo() }.onFailure { error ->
                    AppSentry.capture(error, "Sync: company info failed")
                    AppLogger.warn("Sync: company info failed", error)
                }
            },
            SyncStep("Sincronizando stock settings...") {
                runCatching { stockSettingsRepository.sync() }.onFailure { error ->
                    AppSentry.capture(error, "Sync: stock settings failed")
                    AppLogger.warn("Sync: stock settings failed", error)
                }
            },
            SyncStep("Sincronizando tasas de cambio...") {
                AppSentry.breadcrumb("Sync: exchange rates")
                val enabledCurrencies =
                    runCatching { exchangeRateRepo.getEnabledCurrencyCodes() }.getOrElse { emptyList() }
                val normalized =
                    enabledCurrencies.filter { it.isNotBlank() }.map { it.uppercase() }.distinct()
                runCatching { exchangeRateRepo.syncRatesForCurrencies(normalized) }.onFailure { error ->
                    AppSentry.capture(error, "Sync: exchange rates failed")
                    AppLogger.warn("Sync: exchange rates failed", error)
                }
            },
            SyncStep("Sincronizando métodos del perfil POS...") {
                val profile = posProfileDao.getActiveProfile()
                if (profile != null) {
                    runCatching {
                        posProfilePaymentMethodSyncRepository.syncProfilePayments(profile.profileName)
                    }.onFailure { error ->
                        AppSentry.capture(error, "Sync: pos profile payments failed")
                        AppLogger.warn("Sync: pos profile payments failed", error)
                        throw error
                    }
                }
            },
            SyncStep("Sincronizando documentos pendientes...") {
                runPushQueue()
            }
        )
    }

    private data class SyncStep(
        val message: String,
        val action: suspend () -> Unit
    )

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
