package com.erpnext.pos.sync

import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.data.repositories.BootstrapSyncRepository
import com.erpnext.pos.domain.models.SyncLogEntry
import com.erpnext.pos.domain.models.SyncLogStatus
import com.erpnext.pos.domain.policy.DefaultPolicy
import com.erpnext.pos.domain.policy.PolicyInput
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.preferences.BootstrapContextPreferences
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.preferences.SyncLogPreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface ISyncManager {
    val state: StateFlow<SyncState>
    fun fullSync(ttlHours: Int = SyncTTL.DEFAULT_TTL_HOURS, force: Boolean = false)
    fun syncInventory(force: Boolean = false)
    fun cancelSync()
}

@OptIn(ExperimentalTime::class)
class SyncManager(
    private val bootstrapSyncRepository: BootstrapSyncRepository,
    private val syncPreferences: SyncPreferences,
    private val syncLogPreferences: SyncLogPreferences,
    private val generalPreferences: GeneralPreferences,
    private val cashBoxManager: CashBoxManager,
    private val networkMonitor: NetworkMonitor,
    private val sessionRefresher: SessionRefresher,
    private val syncContextProvider: SyncContextProvider,
    private val pushSyncManager: PushSyncRunner,
    private val bootstrapContextPreferences: BootstrapContextPreferences
) : ISyncManager {
    companion object {
        private const val BOOTSTRAP_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
        private const val STEP_COOLDOWN_MS = 1_000L
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncSettingsCache =
        SyncSettings(
            autoSync = true,
            syncOnStartup = true,
            wifiOnly = false,
            lastSyncAt = null,
            useTtl = false,
            ttlHours = SyncTTL.DEFAULT_TTL_HOURS
        )
    private var lastSyncAttemptAt: Long? = null
    private val minSyncIntervalMillis = 2 * 60 * 1000L
    private var offlineModeCache: Boolean = false

    private val _state = MutableStateFlow<SyncState>(SyncState.IDLE)
    override val state: StateFlow<SyncState> = _state.asStateFlow()
    private var syncJob: Job? = null
    private val bootstrapMutex = Mutex()

    init {
        observeSyncSettings()
        observeOfflineMode()
        observeConnectivity()
        observeBootstrapRefresh()
    }

    @OptIn(ExperimentalTime::class)
    override fun fullSync(ttlHours: Int, force: Boolean) {
        if (offlineModeCache) {
            _state.value = SyncState.ERROR("Modo offline activo. Desactívalo para sincronizar.")
            AppLogger.info("SyncManager.fullSync skipped: offline mode enabled")
            return
        }
        if (_state.value is SyncState.SYNCING || syncJob?.isActive == true) return
        val effectiveTtlHours = ttlHours.coerceIn(1, 168)
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force) {
            val lastAttempt = lastSyncAttemptAt
            if (lastAttempt != null && now - lastAttempt < minSyncIntervalMillis) {
                AppLogger.info(
                    "SyncManager.fullSync skipped: within min interval " +
                        "(elapsedMs=${now - lastAttempt}, minMs=$minSyncIntervalMillis, force=$force)"
                )
                return
            }
            if (syncSettingsCache.useTtl &&
                !SyncTTL.isExpired(syncSettingsCache.lastSyncAt, effectiveTtlHours)
            ) {
                AppLogger.info(
                    "SyncManager.fullSync skipped: TTL not expired " +
                        "(ttlHours=$effectiveTtlHours, lastSyncAt=${syncSettingsCache.lastSyncAt})"
                )
                return
            }
        }
        lastSyncAttemptAt = now

        syncJob = scope.launch {
            AppSentry.breadcrumb(
                message = "SyncManager.fullSync start",
                category = "sync.full",
                data = mapOf(
                    "ttl_hours" to effectiveTtlHours.toString(),
                    "force" to force.toString()
                )
            )
            AppLogger.info("SyncManager.fullSync start (ttl=$effectiveTtlHours)")
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
            val steps = buildFullSyncSteps(effectiveTtlHours)
            LoadingIndicator.start(
                message = "Sincronizando datos...",
                progress = 0f,
                currentStep = 0,
                totalSteps = steps.size
            )
            try {
                for (index in steps.indices) {
                    val step = steps[index]
                    ensureActive()
                    val currentStep = index + 1
                    val stepStartedAt = Clock.System.now().toEpochMilliseconds()
                    AppLogger.info(
                        "SyncManager.fullSync step $currentStep/${steps.size} started: ${step.label}"
                    )
                    updateSyncProgress(
                        message = step.message,
                        currentStep = currentStep,
                        totalSteps = steps.size
                    )
                    val result = runStepWithRetry(step)
                    val stepDurationMs =
                        (Clock.System.now().toEpochMilliseconds() - stepStartedAt).coerceAtLeast(0)
                    var shouldAbort = false
                    if (result != null) {
                        failures.add("${step.label}: $result")
                        AppLogger.warn(
                            "SyncManager.fullSync step $currentStep/${steps.size} failed after " +
                                "${stepDurationMs}ms: ${step.label} -> $result"
                        )
                        if (step.haltOnFailure) {
                            AppLogger.warn("SyncManager.fullSync aborted on critical step ${step.label}")
                            shouldAbort = true
                        }
                    } else {
                        AppLogger.info(
                            "SyncManager.fullSync step $currentStep/${steps.size} finished in " +
                                "${stepDurationMs}ms: ${step.label}"
                        )
                    }
                    LoadingIndicator.update(progress = currentStep.toFloat() / steps.size.toFloat())
                    if (shouldAbort) break
                    if (index < steps.lastIndex) {
                        delay(STEP_COOLDOWN_MS)
                    }
                }
                if (failures.isEmpty()) {
                    _state.value = SyncState.SUCCESS
                    AppSentry.breadcrumb("SyncManager.fullSync success")
                    AppLogger.info("SyncManager.fullSync success")
                } else {
                    val firstFailure = failures.firstOrNull().orEmpty()
                    val summary = if (firstFailure.isNotBlank()) {
                        "Sincronización parcial: ${failures.size} de ${steps.size} fallaron. " +
                                "Primer error: $firstFailure"
                    } else {
                        "Sincronización parcial: ${failures.size} de ${steps.size} fallaron."
                    }
                    _state.value = SyncState.ERROR(summary)
                    AppSentry.breadcrumb(summary)
                    AppLogger.warn("SyncManager.fullSync partial: ${failures.joinToString(" | ")}")
                }
                syncPreferences.setLastSyncAt(Clock.System.now().toEpochMilliseconds())
            } catch (e: CancellationException) {
                val message = "Sincronización cancelada."
                _state.value = SyncState.ERROR(message)
                AppLogger.warn("SyncManager.fullSync cancelled", e)
            } catch (e: Exception) {
                e.printStackTrace()
                AppSentry.capture(
                    throwable = e,
                    message = "SyncManager.fullSync failed",
                    tags = mapOf("component" to "sync.full"),
                    extras = mapOf(
                        "ttl_hours" to effectiveTtlHours.toString(),
                        "force" to force.toString()
                    )
                )
                AppLogger.warn("SyncManager.fullSync failed", e, reportToSentry = false)
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
        if (offlineModeCache) {
            AppLogger.info("SyncManager.syncInventory skipped: offline mode enabled")
            return
        }
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

        AppLogger.info("SyncManager.syncInventory skipped: pull deshabilitado en modo local-first")
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

    private suspend fun runPushQueue(
        updateProgress: Boolean,
        trigger: String
    ): PushQueueReport {
        if (offlineModeCache) {
            AppLogger.warn("SyncManager: push queue skipped because offline mode is enabled")
            return PushQueueReport.EMPTY
        }
        if (!cashBoxManager.cashboxState.value) {
            AppLogger.warn(
                "SyncManager: cashbox is closed, but pending push will continue with fallback context"
            )
        }
        val ctx = resolvePushContext() ?: return PushQueueReport.EMPTY
        val syncing = _state.value as? SyncState.SYNCING
        val currentStep = syncing?.currentStep ?: 1
        val totalSteps = syncing?.totalSteps ?: 1
        if (updateProgress) {
            updateSyncProgress("Sincronizando pendientes...", currentStep, totalSteps)
        }
        try {
            val report = pushSyncManager.runPushQueue(ctx) { docType ->
                if (updateProgress) {
                    updateSyncProgressAsync("Sincronizando $docType...", currentStep, totalSteps)
                } else {
                    AppLogger.info("SyncManager.pushQueue($trigger): $docType")
                }
            }
            logPushReport(trigger, report)
            return report
        } catch (e: Throwable) {
            AppSentry.capture(e, "SyncManager.pushQueue failed")
            AppLogger.warn("SyncManager.pushQueue failed", e, reportToSentry = false)
            throw e
        }
    }

    private suspend fun resolvePushContext(): SyncContext? {
        val resolved = syncContextProvider.buildContext()
        if (resolved != null) return resolved
        val base = cashBoxManager.getContext() ?: cashBoxManager.resolveContextForSync()
        if (base == null) {
            AppLogger.warn("SyncManager: push queue skipped because context is not ready")
            return null
        }
        val instanceId = base.company.ifBlank { base.profileName }.ifBlank { base.username }
        val companyId = base.company.ifBlank { base.profileName }
        if (instanceId.isBlank() || companyId.isBlank()) {
            AppLogger.warn("SyncManager: push queue skipped because instance/company is blank")
            return null
        }
        return SyncContext(
            instanceId = instanceId,
            companyId = companyId,
            territoryId = base.territory ?: base.route ?: "",
            warehouseId = base.warehouse ?: "",
            priceList = base.priceList ?: base.currency,
            fromDate = resolveModifiedSinceDate()
                ?: DefaultPolicy(PolicyInput(3)).invoicesFromDate()
        )
    }

    private fun logPushReport(trigger: String, report: PushQueueReport) {
        if (!report.hasConflicts) return
        val sample = report.conflicts
            .take(3)
            .joinToString(" | ") { conflict ->
                val remote = conflict.remoteId?.takeIf { it.isNotBlank() } ?: "remote:unknown"
                "${conflict.docType} local:${conflict.localId} -> $remote"
            }
        val message = "Push detectó ${report.conflictCount} registro(s) ya existentes en remoto."
        AppLogger.warn("SyncManager.$trigger: $message $sample")
        AppSentry.breadcrumb(
            message = "SyncManager.push conflict detected",
            category = "sync.push",
            data = mapOf(
                "trigger" to trigger,
                "conflict_count" to report.conflictCount.toString(),
                "sample" to sample
            )
        )
    }

    private suspend fun updateSyncProgress(message: String, currentStep: Int, totalSteps: Int) {
        val progressPercent = ((currentStep.toFloat() / totalSteps.toFloat()) * 100f).toInt()
        _state.value = SyncState.SYNCING(
            message = "$message ($progressPercent%)",
            currentStep = currentStep,
            totalSteps = totalSteps
        )
        LoadingIndicator.update(
            message = "$message ($currentStep/$totalSteps - $progressPercent%)",
            progress = currentStep.toFloat() / totalSteps.toFloat(),
            currentStep = currentStep,
            totalSteps = totalSteps
        )
    }

    private fun updateSyncProgressAsync(message: String, currentStep: Int, totalSteps: Int) {
        val progressPercent = ((currentStep.toFloat() / totalSteps.toFloat()) * 100f).toInt()
        _state.value = SyncState.SYNCING(
            message = "$message ($progressPercent%)",
            currentStep = currentStep,
            totalSteps = totalSteps
        )
        scope.launch {
            LoadingIndicator.update(
                message = "$message ($currentStep/$totalSteps - $progressPercent%)",
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

    private fun buildFullSyncSteps(@Suppress("UNUSED_PARAMETER") ttlHours: Int): List<SyncStep> {
        var snapshot: BootstrapSyncRepository.Snapshot? = null
        val bootstrapSections = bootstrapSyncRepository.orderedSections()
        val bootstrapSteps = mutableListOf<SyncStep>()
        bootstrapSteps += SyncStep(
            label = "Pendientes",
            message = "Sincronizando pendientes antes de actualizar...",
            haltOnFailure = true
        ) {
            runPushQueue(updateProgress = true, trigger = "full_sync_pre_pull")
        }
        bootstrapSteps += SyncStep(
            label = "Bootstrap Fetch",
            message = "Descargando datos desde ERP...",
            haltOnFailure = true
        ) {
            val ctx = cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
            val profileName = ctx?.profileName?.trim().orEmpty()
            val activeCashbox = cashBoxManager.getActiveCashboxWithDetails()
            val openingEntryId = activeCashbox?.cashbox?.openingEntryId?.trim().orEmpty()
            val fromDate = resolveModifiedSinceDate()
            bootstrapContextPreferences.update(
                profileName = profileName,
                posOpeningEntry = openingEntryId,
                fromDate = fromDate,
                lastRequestAt = Clock.System.now().toEpochMilliseconds(),
                lastError = ""
            )
            try {
                val fetchedSnapshot = bootstrapSyncRepository.fetchSnapshot(
                    profileName = profileName.takeIf { it.isNotBlank() }
                )
                snapshot = fetchedSnapshot
                val remoteMonthlyTarget = fetchedSnapshot.data.context?.monthlySalesTarget
                    ?.takeIf { it > 0.0 }
                bootstrapContextPreferences.update(
                    monthlySalesTarget = remoteMonthlyTarget,
                    replaceMonthlySalesTarget = remoteMonthlyTarget != null,
                    lastSuccessAt = Clock.System.now().toEpochMilliseconds()
                )
            } catch (e: Exception) {
                bootstrapContextPreferences.update(lastError = e.message ?: "Bootstrap Fetch failed")
                throw e
            }
        }
        bootstrapSections.forEach { section ->
            bootstrapSteps += SyncStep(
                label = section.label,
                message = section.message
            ) {
                val resolved = snapshot
                    ?: throw IllegalStateException("No se pudo descargar el snapshot de sync.bootstrap")
                bootstrapSyncRepository.persistSection(resolved, section)
            }
        }
        bootstrapSteps += SyncStep(
            label = "Caja",
            message = "Reconciliando estado de caja..."
        ) {
            cashBoxManager.initializeContext()
        }
        return bootstrapSteps
    }

    private data class SyncStep(
        val label: String,
        val message: String,
        val haltOnFailure: Boolean = false,
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

    private fun observeOfflineMode() {
        scope.launch {
            generalPreferences.offlineMode.collect { enabled ->
                offlineModeCache = enabled
                if (enabled && _state.value is SyncState.SYNCING) {
                    cancelSync()
                }
            }
        }
    }

    private fun observeConnectivity() {
        scope.launch {
            var wasConnected = false
            networkMonitor.isConnected.collect { connected ->
                if (connected && !wasConnected && shouldAutoSyncOnConnection()) {
                    fullSync(ttlHours = syncSettingsCache.ttlHours, force = false)
                }
                wasConnected = connected
            }
        }
    }

    private fun observeBootstrapRefresh() {
        scope.launch {
            while (true) {
                delay(BOOTSTRAP_REFRESH_INTERVAL_MS)
                refreshBootstrapSnapshot(trigger = "interval_5m")
            }
        }
    }

    private suspend fun refreshBootstrapSnapshot(trigger: String): Boolean {
        return bootstrapMutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            if (_state.value is SyncState.SYNCING || syncJob?.isActive == true) {
                AppLogger.info("SyncManager.bootstrap skipped ($trigger): sync in progress")
                return@withLock false
            }
            if (offlineModeCache) {
                AppLogger.info("SyncManager.bootstrap skipped ($trigger): offline mode enabled")
                return@withLock false
            }
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                AppLogger.info("SyncManager.bootstrap skipped ($trigger): offline")
                return@withLock false
            }
            if (!sessionRefresher.ensureValidSession()) {
                AppLogger.warn("SyncManager.bootstrap skipped ($trigger): invalid session")
                return@withLock false
            }
            val bootstrapSnapshot = bootstrapContextPreferences.load()
            val effectiveTtlHours = syncSettingsCache.ttlHours.coerceIn(1, 168)
            if (syncSettingsCache.useTtl &&
                trigger.startsWith("interval") &&
                !SyncTTL.isExpired(
                    bootstrapSnapshot.lastSuccessAt ?: bootstrapSnapshot.lastRequestAt,
                    effectiveTtlHours
                )
            ) {
                AppLogger.info(
                    "SyncManager.bootstrap skipped ($trigger): TTL not expired " +
                        "(ttlHours=$effectiveTtlHours, lastSuccessAt=${bootstrapSnapshot.lastSuccessAt}, " +
                        "lastRequestAt=${bootstrapSnapshot.lastRequestAt})"
                )
                AppSentry.breadcrumb(
                    message = "SyncManager.bootstrap skipped TTL",
                    category = "sync.bootstrap",
                    data = mapOf(
                        "trigger" to trigger,
                        "ttl_hours" to effectiveTtlHours.toString()
                    )
                )
                return@withLock false
            }
            return@withLock runCatching {
                runPushQueue(
                    updateProgress = false,
                    trigger = "bootstrap_refresh_$trigger"
                )
                val ctx = cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
                val profileName = ctx?.profileName?.trim().orEmpty()
                val activeCashbox = cashBoxManager.getActiveCashboxWithDetails()
                val openingEntryId = activeCashbox?.cashbox?.openingEntryId?.trim().orEmpty()
                val fromDate = resolveModifiedSinceDate()
                bootstrapContextPreferences.update(
                    profileName = profileName,
                    posOpeningEntry = openingEntryId,
                    fromDate = fromDate,
                    lastRequestAt = now,
                    lastError = ""
                )
                AppSentry.breadcrumb(
                    message = "SyncManager.bootstrap refresh start",
                    category = "sync.bootstrap",
                    data = mapOf(
                        "trigger" to trigger,
                        "ttl_enabled" to syncSettingsCache.useTtl.toString(),
                        "ttl_hours" to effectiveTtlHours.toString()
                    )
                )
                val snapshot = bootstrapSyncRepository.fetchSnapshot(
                    profileName = profileName.takeIf { it.isNotBlank() }
                )
                val remoteMonthlyTarget = snapshot.data.context?.monthlySalesTarget
                    ?.takeIf { it > 0.0 }
                bootstrapContextPreferences.update(
                    monthlySalesTarget = remoteMonthlyTarget,
                    replaceMonthlySalesTarget = remoteMonthlyTarget != null,
                    lastSuccessAt = Clock.System.now().toEpochMilliseconds()
                )
                bootstrapSyncRepository.persistAll(snapshot)
                cashBoxManager.initializeContext()
                AppLogger.info("SyncManager.bootstrap refreshed ($trigger)")
                true
            }.onFailure {
                bootstrapContextPreferences.update(lastError = it.message ?: "Bootstrap refresh failed")
                AppSentry.capture(
                    throwable = it,
                    message = "SyncManager.bootstrap failed ($trigger)",
                    tags = mapOf("component" to "sync.bootstrap", "trigger" to trigger),
                    extras = mapOf(
                        "ttl_enabled" to syncSettingsCache.useTtl.toString(),
                        "ttl_hours" to effectiveTtlHours.toString()
                    )
                )
                AppLogger.warn("SyncManager.bootstrap failed ($trigger)", it, reportToSentry = false)
            }.getOrElse { false }
        }
    }

    private suspend fun resolveModifiedSinceDate(): String? {
        val lastSyncAt = syncSettingsCache.lastSyncAt ?: return syncContextProvider.buildContext()?.fromDate
        return Instant.fromEpochMilliseconds(lastSyncAt)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
    }

    private fun shouldAutoSyncOnConnection(): Boolean {
        if (offlineModeCache) return false
        if (!syncSettingsCache.autoSync) return false
        if (syncSettingsCache.wifiOnly) return false
        return _state.value !is SyncState.SYNCING
    }
}
