package com.erpnext.pos.views.home

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoLocalUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.domain.usecases.HomeMetricInput
import com.erpnext.pos.domain.usecases.LoadHomeMetricsUseCase
import com.erpnext.pos.domain.usecases.LoadInventoryAlertsUseCase
import com.erpnext.pos.domain.usecases.InventoryAlertInput
import com.erpnext.pos.domain.usecases.LogoutUseCase
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.sync.GateResult
import com.erpnext.pos.sync.PosProfileGate
import com.erpnext.pos.sync.OpeningGate
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.PaymentModeWithAmount
import com.erpnext.pos.views.reconciliation.ReconciliationMode
import com.erpnext.pos.utils.notifications.notifySystem
import com.erpnext.pos.utils.notifications.scheduleDailyInventoryReminder
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.utils.normalizeCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeViewModel(
    private val fetchUserInfoUseCase: FetchUserInfoUseCase,
    private val fetchPosProfileUseCase: FetchPosProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val fetchPosProfileInfoLocalUseCase: FetchPosProfileInfoLocalUseCase,
    private val contextManager: CashBoxManager,
    private val posProfileDao: POSProfileDao,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val syncManager: SyncManager,
    private val syncPreferences: SyncPreferences,
    private val navManager: NavigationManager,
    private val loadHomeMetricsUseCase: LoadHomeMetricsUseCase,
    private val loadInventoryAlertsUseCase: LoadInventoryAlertsUseCase,
    private val posProfileGate: PosProfileGate,
    private val openingGate: OpeningGate,
    private val homeRefreshController: HomeRefreshController,
    private val sessionRefresher: SessionRefresher,
    private val syncContextProvider: SyncContextProvider,
    private val generalPreferences: GeneralPreferences,
    private val exchangeRateLocalSource: ExchangeRateLocalSource
) : BaseViewModel() {
    private val _stateFlow: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Loading)
    val stateFlow = _stateFlow.asStateFlow()

    val syncState: StateFlow<SyncState> = syncManager.state
    private val _syncSettings = MutableStateFlow(
        SyncSettings(
            autoSync = true,
            syncOnStartup = true,
            wifiOnly = false,
            lastSyncAt = null,
            useTtl = false
        )
    )
    val syncSettings: StateFlow<SyncSettings> = _syncSettings.asStateFlow()
    private val _homeMetrics = MutableStateFlow(HomeMetrics())
    val homeMetrics: StateFlow<HomeMetrics> = _homeMetrics.asStateFlow()

    private val _inventoryAlertMessage = MutableStateFlow<String?>(null)
    val inventoryAlertMessage: StateFlow<String?> = _inventoryAlertMessage.asStateFlow()

    private val _openingState = MutableStateFlow(CashboxOpeningProfileState())
    val openingState: StateFlow<CashboxOpeningProfileState> = _openingState.asStateFlow()

    private var userInfo: UserBO = UserBO()
    private var posProfiles: List<POSProfileSimpleBO> = emptyList()
    private var lastInventoryProfile: String? = null

    init {
        viewModelScope.launch {
            contextManager.initializeContext()
        }
        viewModelScope.launch {
            syncPreferences.settings.collectLatest { settings ->
                _syncSettings.value = settings
            }
        }
        viewModelScope.launch {
            isCashboxOpen().collectLatest {
                if (it && _syncSettings.value.syncOnStartup) {
                    startInitialSync()
                }
                if (it) {
                    val profile = contextManager.getContext()?.profileName
                    if (!profile.isNullOrBlank() && profile != lastInventoryProfile) {
                        lastInventoryProfile = profile
                        syncManager.syncInventory(force = true)
                    }
                }
            }
        }
        viewModelScope.launch {
            syncState.collectLatest { state ->
                if (state is SyncState.SUCCESS) {
                    refreshMetrics()
                }
            }
        }
        viewModelScope.launch {
            generalPreferences.salesTargetMonthly.collectLatest {
                refreshSalesTarget()
            }
        }
        viewModelScope.launch {
            homeRefreshController.events.collectLatest {
                loadInitialData()
            }
        }
        viewModelScope.launch {
            while (true) {
                val hour = generalPreferences.getInventoryAlertHour()
                val minute = generalPreferences.getInventoryAlertMinute()
                val delayMs = millisUntilNextAlert(hour, minute)
                delay(delayMs)
                refreshInventoryAlerts()
            }
        }
        loadInitialData()
    }

    fun startInitialSync() {
        if (!_syncSettings.value.autoSync) return
        executeUseCase(
            action = {
                if (sessionRefresher.ensureValidSession()) {
                    syncManager.fullSync(force = false)
                }
            },
            exceptionHandler = { it.printStackTrace() })
    }

    fun syncNow() {
        executeUseCase(
            action = {
                if (sessionRefresher.ensureValidSession()) {
                    syncManager.fullSync(force = true)
                }
            },
            exceptionHandler = { it.printStackTrace() })
    }

    fun loadInitialData() {
        _stateFlow.update { HomeState.Loading }
        executeUseCase(action = {
            if (!sessionRefresher.ensureValidSession()) return@executeUseCase
            userInfo = fetchUserInfoUseCase.invoke(null)
            when (val gate = posProfileGate.ensureReady(userInfo.email)) {
                is GateResult.Failed -> error(gate.reason)
                is GateResult.Pending -> error(gate.reason)
                GateResult.Ready -> Unit
            }
            posProfiles = fetchPosProfileUseCase.invoke(userInfo.email)
            _homeMetrics.value =
                loadHomeMetricsUseCase(HomeMetricInput(7, Clock.System.now().toEpochMilliseconds()))
            refreshInventoryAlerts()
            refreshSalesTarget()
            _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
        }, exceptionHandler = { e ->
            _stateFlow.update { HomeState.Error(e.message ?: "Error") }
        })
    }

    fun refreshMetrics() {
        executeUseCase(
            action = {
                _homeMetrics.value = loadHomeMetricsUseCase(
                    HomeMetricInput(
                        7,
                        Clock.System.now().toEpochMilliseconds()
                    )
                )
                refreshInventoryAlerts()
                refreshSalesTarget()
            },
            exceptionHandler = { it.printStackTrace() }
        )
    }

    private fun refreshInventoryAlerts() {
        viewModelScope.launch {
            val ctx = syncContextProvider.buildContext() ?: return@launch
            if (ctx.warehouseId.isBlank()) return@launch
            val alertsEnabled = generalPreferences.getInventoryAlertsEnabled()
            val alertHour = generalPreferences.getInventoryAlertHour()
            val alertMinute = generalPreferences.getInventoryAlertMinute()
            if (!alertsEnabled) {
                _homeMetrics.update { it.copy(inventoryAlerts = emptyList()) }
                scheduleDailyInventoryReminder(
                    false,
                    "Inventory Alerts",
                    "",
                    alertHour,
                    alertMinute
                )
                return@launch
            }
            val alerts = loadInventoryAlertsUseCase(
                InventoryAlertInput(
                    instanceId = ctx.instanceId,
                    companyId = ctx.companyId,
                    warehouseId = ctx.warehouseId,
                    limit = 20
                )
            )
            _homeMetrics.update { it.copy(inventoryAlerts = alerts) }
            val hasAlerts = alerts.isNotEmpty()
            val scheduledMessage =
                if (hasAlerts) "Alertas de inventario pendientes: ${alerts.size} (${ctx.warehouseId})"
                else ""
            scheduleDailyInventoryReminder(
                hasAlerts,
                "Inventory Alerts",
                scheduledMessage,
                alertHour,
                alertMinute
            )
            if (!hasAlerts) return@launch

            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            val lastDate = generalPreferences.getInventoryAlertDate()
            if (lastDate != today) {
                generalPreferences.setInventoryAlertDate(today)
                val message = "Alertas de inventario: ${alerts.size} (${ctx.warehouseId})"
                _inventoryAlertMessage.value = message
                notifySystem("Inventory Alerts", message)
            }
        }
    }

    private fun millisUntilNextAlert(hour: Int, minute: Int): Long {
        val tz = TimeZone.currentSystemDefault()
        val nowInstant = Clock.System.now()
        val nowLocal = nowInstant.toLocalDateTime(tz)
        val targetToday = nowLocal.date.atTime(hour, minute)
        val targetLocal = if (targetToday <= nowLocal) {
            nowLocal.date.plus(DatePeriod(days = 1)).atTime(hour, minute)
        } else {
            targetToday
        }
        val targetInstant = targetLocal.toInstant(tz)
        return (targetInstant.toEpochMilliseconds() - nowInstant.toEpochMilliseconds())
            .coerceAtLeast(1_000L)
    }

    private fun refreshSalesTarget() {
        viewModelScope.launch {
            val ctx = contextManager.getContext() ?: return@launch
            val monthly = generalPreferences.getSalesTargetMonthly()
            if (monthly <= 0.0) {
                _homeMetrics.update { it.copy(salesTarget = null) }
                return@launch
            }

            val baseCurrency = normalizeCurrency(ctx.companyCurrency)
            val secondaryCurrency = normalizeCurrency(ctx.currency)
                .takeIf { it != baseCurrency }

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val daysInMonth = daysInMonth(now.year, now.monthNumber)
            val weeksInMonth = weeksInMonth(now.year, now.monthNumber, daysInMonth)

            val daily = if (daysInMonth > 0) monthly / daysInMonth.toDouble() else 0.0
            val weekly = if (weeksInMonth > 0) monthly / weeksInMonth.toDouble() else 0.0

            val rate = if (secondaryCurrency != null) {
                resolveLocalRate(baseCurrency, secondaryCurrency)
            } else {
                null
            }
            val rateValue = rate?.rate
            val stale = rate?.let { isRateStale(it.lastSyncedAt) } ?: false

            val monthlySecondary = rateValue?.let { monthly * it }
            val weeklySecondary = rateValue?.let { weekly * it }
            val dailySecondary = rateValue?.let { daily * it }

            _homeMetrics.update {
                it.copy(
                    salesTarget = SalesTargetMetric(
                        baseCurrency = baseCurrency,
                        secondaryCurrency = secondaryCurrency,
                        monthlyBase = monthly,
                        weeklyBase = weekly,
                        dailyBase = daily,
                        monthlySecondary = monthlySecondary,
                        weeklySecondary = weeklySecondary,
                        dailySecondary = dailySecondary,
                        conversionStale = stale
                    )
                )
            }
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun weeksInMonth(year: Int, month: Int, daysInMonth: Int): Int {
        if (daysInMonth <= 0) return 0
        val firstDay = LocalDate(year, month, 1)
        var mondays = 0
        for (i in 0 until daysInMonth) {
            val day = firstDay.plus(DatePeriod(days = i))
            if (day.dayOfWeek == DayOfWeek.MONDAY) {
                mondays++
            }
        }
        return if (firstDay.dayOfWeek == DayOfWeek.MONDAY) mondays else mondays + 1
    }

    private suspend fun resolveLocalRate(from: String, to: String): RateInfo? {
        val direct = exchangeRateLocalSource.getRate(from, to)
        if (direct != null) {
            return RateInfo(direct.rate, direct.lastSyncedAt)
        }
        val reverse = exchangeRateLocalSource.getRate(to, from) ?: return null
        if (reverse.rate == 0.0) return null
        return RateInfo(1 / reverse.rate, reverse.lastSyncedAt)
    }

    private fun isRateStale(lastSyncedAt: Long): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        return now - lastSyncedAt > sevenDaysMs
    }

    private data class RateInfo(
        val rate: Double,
        val lastSyncedAt: Long
    )

    fun consumeInventoryAlertMessage() {
        _inventoryAlertMessage.value = null
    }

    fun isCashboxOpen(): StateFlow<Boolean> = contextManager.cashboxState

    fun resetToInitialState() {
        _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
    }

    fun logout() {
        executeUseCase(action = {
            logoutUseCase.invoke(null)
            _stateFlow.update { HomeState.Logout }
            navManager.navigateTo(NavRoute.Login)
        }, exceptionHandler = { it.printStackTrace() })
    }

    fun openSettings() {
        navManager.navigateTo(NavRoute.Settings)
    }

    fun openReconciliation() {
        navManager.navigateTo(NavRoute.Reconciliation(ReconciliationMode.Close))
    }

    fun openCloseCashbox() {
        navManager.navigateTo(NavRoute.Reconciliation(ReconciliationMode.Close))
    }

    fun onError(error: String) {
        _stateFlow.update { HomeState.Error(error) }
    }

    fun onPosSelected(pos: POSProfileSimpleBO) {
        _stateFlow.update { HomeState.POSInfoLoading }
        executeUseCase(action = {
            val posProfileInfo = fetchPosProfileInfoLocalUseCase(pos.name)
            _stateFlow.update { HomeState.POSInfoLoaded(posProfileInfo, posProfileInfo.currency) }
        }, exceptionHandler = { it.printStackTrace() })
    }

    fun closeCashbox() {
        viewModelScope.launch {
            contextManager.closeCashBox()
        }
    }

    fun loadOpeningProfile(profileId: String?) {
        if (profileId.isNullOrBlank()) {
            _openingState.update {
                it.copy(profileId = null, methods = emptyList(), cashMethodsByCurrency = emptyMap())
            }
            return
        }
        viewModelScope.launch {
            _openingState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                when (val gate = openingGate.ensureReady(profileId)) {
                    is GateResult.Failed -> error(gate.reason)
                    is GateResult.Pending -> error(gate.reason)
                    GateResult.Ready -> Unit
                }
                val profile = posProfileDao.getPOSProfile(profileId)
                val methods = paymentMethodLocalRepository.getMethodsForProfile(profileId)
                val cashMethods = paymentMethodLocalRepository.getCashMethodsGroupedByCurrency(
                    profileId,
                    profile.currency
                )
                _openingState.update {
                    it.copy(
                        profileId = profile.profileName,
                        company = profile.company,
                        baseCurrency = profile.currency,
                        methods = methods,
                        cashMethodsByCurrency = cashMethods,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { error ->
                _openingState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load profile data"
                    )
                }
            }
        }
    }

    fun openCashbox(entry: POSProfileSimpleBO, amounts: List<PaymentModeWithAmount>) {
        viewModelScope.launch {
            val ctx = contextManager.openCashBox(entry, amounts)
            if (ctx == null) {
                _openingState.update {
                    it.copy(
                        isLoading = false,
                        error = "Sesion expirada. Inicia sesion nuevamente."
                    )
                }
            }
        }
    }
}
