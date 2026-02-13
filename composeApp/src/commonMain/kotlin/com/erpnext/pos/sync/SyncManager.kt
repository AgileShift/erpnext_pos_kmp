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
import com.erpnext.pos.data.repositories.CurrencySettingsRepository
import com.erpnext.pos.sync.PushSyncRunner
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localSource.preferences.SyncLogPreferences
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.domain.policy.DefaultPolicy
import com.erpnext.pos.domain.policy.PolicyInput
import com.erpnext.pos.domain.models.SyncLogEntry
import com.erpnext.pos.domain.models.SyncLogStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ISyncManager {
    val state: StateFlow<SyncState>
    fun fullSync(ttlHours: Int = SyncTTL.DEFAULT_TTL_HOURS, force: Boolean = false)
    fun syncInventory(force: Boolean = false)
    fun cancelSync()
}

class SyncManager(
    private val invoiceRepo: SalesInvoiceRepository,
    private val customerRepo: CustomerRepository,
    private val inventoryRepo: InventoryRepository,
    private val modeOfPaymentRepo: ModeOfPaymentRepository,
    private val posProfilePaymentMethodSyncRepository: PosProfilePaymentMethodSyncRepository,
    private val stockSettingsRepository: com.erpnext.pos.data.repositories.StockSettingsRepository,
    private val currencySettingsRepository: CurrencySettingsRepository,
    private val paymentTermsRepo: PaymentTermsRepository,
    private val deliveryChargesRepo: DeliveryChargesRepository,
    private val contactRepo: ContactRepository,
    private val addressRepo: AddressRepository,
    private val customerGroupRepo: CustomerGroupRepository,
    private val territoryRepo: TerritoryRepository,
    private val exchangeRateRepo: ExchangeRateRepository,
    private val syncPreferences: SyncPreferences,
    private val syncLogPreferences: SyncLogPreferences,
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
    private var syncJob: Job? = null

    init {
        observeSyncSettings()
        observeConnectivity()
    }

    @OptIn(ExperimentalTime::class)
    override fun fullSync(ttlHours: Int, force: Boolean) {
        if (_state.value is SyncState.SYNCING || syncJob?.isActive == true) return

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

        /* TODO: Agregar POS Profiles con sus detalles, esto se removera en una futura iteracion
            Porque los perfiles ya vendran en el initial data y solo los que pertenecen al usuario
         */
        syncJob = scope.launch {
            AppSentry.breadcrumb("SyncManager.fullSync start (ttl=$ttlHours)")
            AppLogger.info("SyncManager.fullSync start (ttl=$ttlHours)")
            val startedAt = Clock.System.now().toEpochMilliseconds()
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

            val failures = mutableListOf<String>()
            val steps = buildFullSyncSteps(ttlHours)
            LoadingIndicator.start(
                message = "Sincronizando datos...",
                progress = 0f,
                currentStep = 0,
                totalSteps = steps.size
            )
            try {
                steps.forEachIndexed { index, step ->
                    ensureActive()
                    val currentStep = index + 1
                    updateSyncProgress(
                        message = step.message,
                        currentStep = currentStep,
                        totalSteps = steps.size
                    )
                    val result = runStepWithRetry(step)
                    if (result != null) {
                        failures.add("${step.label}: $result")
                    }
                    LoadingIndicator.update(progress = currentStep.toFloat() / steps.size.toFloat())
                }
                if (failures.isEmpty()) {
                    _state.value = SyncState.SUCCESS
                    AppSentry.breadcrumb("SyncManager.fullSync success")
                    AppLogger.info("SyncManager.fullSync success")
                } else {
                    val summary = "Sincronización parcial: ${failures.size} de ${steps.size} fallaron."
                    _state.value = SyncState.ERROR(summary)
                    AppSentry.breadcrumb(summary)
                    AppLogger.warn("SyncManager.fullSync partial: ${failures.joinToString(" | ")}")
                }
                syncPreferences.setLastSyncAt(Clock.System.now().toEpochMilliseconds())
            } catch (e: CancellationException) {
                val message = "Sincronización cancelada."
                _state.value = SyncState.ERROR(message)
                AppLogger.warn("SyncManager.fullSync cancelled")
            } catch (e: Exception) {
                e.printStackTrace()
                AppSentry.capture(e, "SyncManager.fullSync failed")
                AppLogger.warn("SyncManager.fullSync failed", e)
                _state.value = SyncState.ERROR("Error durante la sincronización: ${e.message}")
            } finally {
                syncJob = null
                val finishedAt = Clock.System.now().toEpochMilliseconds()
                val status = when {
                    failures.isEmpty() && _state.value is SyncState.SUCCESS -> SyncLogStatus.SUCCESS
                    _state.value is SyncState.ERROR && (_state.value as SyncState.ERROR).message.contains("cancelada") ->
                        SyncLogStatus.CANCELED
                    failures.isNotEmpty() -> SyncLogStatus.PARTIAL
                    else -> SyncLogStatus.ERROR
                }
                val message = when (val st = _state.value) {
                    is SyncState.ERROR -> st.message
                    is SyncState.SUCCESS -> "Sincronización exitosa."
                    else -> "Sincronización finalizada."
                }
                runCatching {
                    syncLogPreferences.append(
                        SyncLogEntry(
                            id = "sync-${startedAt}",
                            startedAt = startedAt,
                            finishedAt = finishedAt,
                            durationMs = (finishedAt - startedAt).coerceAtLeast(0),
                            totalSteps = steps.size,
                            failedSteps = failures,
                            status = status,
                            message = message
                        )
                    )
                }
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
            val startedAt = Clock.System.now().toEpochMilliseconds()
            val failures = mutableListOf<String>()
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
                val errorMessage = runStepWithRetry(
                    SyncStep(
                        label = "Inventario",
                        message = "Sincronizando inventario...",
                        attempts = 2,
                        initialDelayMs = 800L
                    ) {
                        val result = inventoryRepo.sync().filter { it !is Resource.Loading }.first()
                        if (result is Resource.Error) {
                            val error = Exception(
                                result.message ?: "Error al sincronizar inventario"
                            )
                            AppSentry.capture(error, "Sync: inventory only failed")
                            AppLogger.warn("Sync: inventory only failed", error)
                            throw error
                        }
                    }
                )
                if (errorMessage != null) {
                    failures.add("Inventario: $errorMessage")
                }
                LoadingIndicator.update(progress = 1f)
                _state.value = if (failures.isEmpty()) SyncState.SUCCESS
                else SyncState.ERROR("Sincronización parcial: Inventario")
            } catch (e: CancellationException) {
                _state.value = SyncState.ERROR("Sincronización cancelada.")
            } catch (e: Exception) {
                AppSentry.capture(e, "SyncManager.syncInventory failed")
                AppLogger.warn("SyncManager.syncInventory failed", e)
                _state.value = SyncState.ERROR("Error al sincronizar inventario: ${e.message}")
            } finally {
                val finishedAt = Clock.System.now().toEpochMilliseconds()
                val status = when {
                    failures.isEmpty() && _state.value is SyncState.SUCCESS -> SyncLogStatus.SUCCESS
                    _state.value is SyncState.ERROR && (_state.value as SyncState.ERROR).message.contains("cancelada") ->
                        SyncLogStatus.CANCELED
                    failures.isNotEmpty() -> SyncLogStatus.PARTIAL
                    else -> SyncLogStatus.ERROR
                }
                val message = when (val st = _state.value) {
                    is SyncState.ERROR -> st.message
                    is SyncState.SUCCESS -> "Sincronización de inventario exitosa."
                    else -> "Sincronización de inventario finalizada."
                }
                runCatching {
                    syncLogPreferences.append(
                        SyncLogEntry(
                            id = "sync-inventory-${startedAt}",
                            startedAt = startedAt,
                            finishedAt = finishedAt,
                            durationMs = (finishedAt - startedAt).coerceAtLeast(0),
                            totalSteps = 1,
                            failedSteps = failures,
                            status = status,
                            message = message
                        )
                    )
                }
                LoadingIndicator.stop()
                delay(2000)
                _state.value = SyncState.IDLE
            }
        }
    }

    override fun cancelSync() {
        if (_state.value !is SyncState.SYNCING) return
        syncJob?.cancel(CancellationException("Sync cancelled by user"))
        scope.launch {
            val syncing = _state.value as? SyncState.SYNCING
            val currentStep = syncing?.currentStep ?: 1
            val totalSteps = syncing?.totalSteps ?: 1
            updateSyncProgress("Cancelando sincronización...", currentStep, totalSteps)
            LoadingIndicator.stop()
            _state.value = SyncState.ERROR("Sincronización cancelada.")
            delay(1500)
            _state.value = SyncState.IDLE
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

    private suspend fun runStepWithRetry(step: SyncStep): String? {
        var attempt = 0
        var delayMs = step.initialDelayMs
        while (attempt < step.attempts) {
            try {
                step.action()
                return null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempt += 1
                if (attempt >= step.attempts) {
                    return e.message ?: "Error al sincronizar ${step.label.lowercase()}"
                }
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(step.maxDelayMs)
            }
        }
        return null
    }

    private fun buildFullSyncSteps(ttlHours: Int): List<SyncStep> {
        return listOf(
            SyncStep(label = "Métodos de pago", message = "Sincronizando métodos de pago...") {
                AppSentry.breadcrumb("Sync: mode of payment")
                val result = modeOfPaymentRepo.sync(ttlHours).filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar métodos de pago")
                    AppSentry.capture(error, "Sync: mode of payment failed")
                    AppLogger.warn("Sync: mode of payment failed", error)
                    throw error
                }
            },
            SyncStep(label = "Clientes", message = "Sincronizando clientes...") {
                AppSentry.breadcrumb("Sync: customers")
                val result = customerRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar clientes")
                    AppSentry.capture(error, "Sync: customers failed")
                    AppLogger.warn("Sync: customers failed", error)
                    throw error
                }
            },
            SyncStep(label = "Categorías", message = "Sincronizando categorías de productos...") {
                AppSentry.breadcrumb("Sync: categories")
                val result = inventoryRepo.getCategories().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar categorías")
                    AppSentry.capture(error, "Sync: categories failed")
                    AppLogger.warn("Sync: categories failed", error)
                    throw error
                }
            },
            SyncStep(label = "Inventario", message = "Sincronizando inventario...") {
                AppSentry.breadcrumb("Sync: inventory")
                val result = inventoryRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar inventario")
                    AppSentry.capture(error, "Sync: inventory failed")
                    AppLogger.warn("Sync: inventory failed", error)
                    throw error
                }
            },
            SyncStep(label = "Facturas", message = "Sincronizando facturas...") {
                AppSentry.breadcrumb("Sync: invoices")
                val result = invoiceRepo.sync().filter { it !is Resource.Loading }.first()
                if (result is Resource.Error) {
                    val error = Exception(result.message ?: "Error al sincronizar facturas")
                    AppSentry.capture(error, "Sync: invoices failed")
                    AppLogger.warn("Sync: invoices failed", error)
                    throw error
                }
                // Recalcula resúmenes de clientes luego de cambios en facturas.
                runCatching { customerRepo.rebuildAllCustomerSummaries() }.onFailure { error ->
                    AppSentry.capture(error, "Sync: rebuild customer summaries failed")
                    AppLogger.warn("Sync: rebuild customer summaries failed", error)
                }
            },
            SyncStep(label = "Términos de pago", message = "Sincronizando términos de pago...") {
                AppSentry.breadcrumb("Sync: payment terms")
                runCatching { paymentTermsRepo.fetchPaymentTerms() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar términos de pago")
                    AppSentry.capture(exception, "Sync: payment terms failed")
                    AppLogger.warn("Sync: payment terms failed", exception)
                    throw exception
                }
            },
            SyncStep(label = "Grupos de cliente", message = "Sincronizando grupos de cliente...") {
                AppSentry.breadcrumb("Sync: customer groups")
                runCatching { customerGroupRepo.fetchCustomerGroups() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar grupos de cliente")
                    AppSentry.capture(exception, "Sync: customer groups failed")
                    AppLogger.warn("Sync: customer groups failed", exception)
                    throw exception
                }
            },
            SyncStep(label = "Territorios", message = "Sincronizando territorios...") {
                AppSentry.breadcrumb("Sync: territories")
                runCatching { territoryRepo.fetchTerritories() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar territorios")
                    AppSentry.capture(exception, "Sync: territories failed")
                    AppLogger.warn("Sync: territories failed", exception)
                    throw exception
                }
            },
            SyncStep(label = "Cargos de envío", message = "Sincronizando cargos de envío...") {
                AppSentry.breadcrumb("Sync: delivery charges")
                runCatching { deliveryChargesRepo.fetchDeliveryCharges() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar cargos de envío")
                    AppSentry.capture(exception, "Sync: delivery charges failed")
                    AppLogger.warn("Sync: delivery charges failed", exception)
                    throw exception
                }
            },
            SyncStep(label = "Contactos", message = "Sincronizando contactos...") {
                AppSentry.breadcrumb("Sync: contacts")
                runCatching { contactRepo.fetchCustomerContacts() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar contactos")
                    AppSentry.capture(exception, "Sync: contacts failed")
                    AppLogger.warn("Sync: contacts failed", exception)
                    throw exception
                }
            },
            SyncStep(label = "Direcciones", message = "Sincronizando direcciones...") {
                AppSentry.breadcrumb("Sync: addresses")
                runCatching { addressRepo.fetchCustomerAddresses() }.getOrElse { error ->
                    val exception = Exception(error.message ?: "Error al sincronizar direcciones")
                    AppSentry.capture(exception, "Sync: addresses failed")
                    AppLogger.warn("Sync: addresses failed", exception)
                    throw exception
                }
            },
            SyncStep(label = "Empresa", message = "Sincronizando información de la empresa...", attempts = 1) {
                AppSentry.breadcrumb("Sync: company info")
                runCatching { companyInfoRepo.getCompanyInfo() }.onFailure { error ->
                    AppSentry.capture(error, "Sync: company info failed")
                    AppLogger.warn("Sync: company info failed", error)
                }
            },
            SyncStep(label = "Stock settings", message = "Sincronizando stock settings...", attempts = 1) {
                runCatching { stockSettingsRepository.sync() }.onFailure { error ->
                    AppSentry.capture(error, "Sync: stock settings failed")
                    AppLogger.warn("Sync: stock settings failed", error)
                }
            },
            SyncStep(label = "Moneda", message = "Sincronizando configuración de moneda...", attempts = 1) {
                runCatching { currencySettingsRepository.sync() }.onFailure { error ->
                    AppSentry.capture(error, "Sync: currency settings failed")
                    AppLogger.warn("Sync: currency settings failed", error)
                }
            },
            SyncStep(label = "Tasas de cambio", message = "Sincronizando tasas de cambio...") {
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
            SyncStep(label = "Métodos POS", message = "Sincronizando métodos del perfil POS...") {
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
            SyncStep(label = "Pendientes", message = "Sincronizando documentos pendientes...") {
                runPushQueue()
            }
        )
    }

    private data class SyncStep(
        val label: String,
        val message: String,
        val attempts: Int = 3,
        val initialDelayMs: Long = 600L,
        val maxDelayMs: Long = 6_000L,
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
