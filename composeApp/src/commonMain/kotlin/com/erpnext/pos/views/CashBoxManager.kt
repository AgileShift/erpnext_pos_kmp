package com.erpnext.pos.views

import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.data.mappers.buildClosingEntryDto
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.data.repositories.ClosingEntrySyncRepository
import com.erpnext.pos.data.repositories.CurrencySettingsRepository
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.data.repositories.OpeningEntrySyncRepository
import com.erpnext.pos.data.repositories.PosOpeningRepository
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CashboxWithDetails
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryLinkEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.localSource.preferences.BootstrapContextPreferences
import com.erpnext.pos.localSource.preferences.ExchangeRatePreferences
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.BootstrapClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.buildPaymentReconciliationSeeds
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.toErpDateTime
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class PaymentModeWithAmount(
    val mode: PaymentModesBO, val amount: Double
)

data class POSContext(
    val cashier: UserBO,
    val username: String,
    val profileName: String,
    val company: String,
    val companyCurrency: String,
    val allowNegativeStock: Boolean,
    val warehouse: String?,
    val route: String?,
    val territory: String?,
    val priceList: String?,
    val isCashBoxOpen: Boolean,
    val cashboxId: Long?,
    val incomeAccount: String?,
    val expenseAccount: String?,
    val branch: String?,
    val applyDiscountOn: String?,
    val currency: String,
    val partyAccountCurrency: String,
    val monthlySalesTarget: Double?,
    val defaultReceivableAccount: String?,
    val defaultReceivableAccountCurrency: String?,
    val exchangeRate: Double,
    val allowedCurrencies: List<POSCurrencyOption>,
    val paymentModes: List<POSPaymentModeOption>
)

class CashBoxManager(
    private val api: APIService,
    private val profileDao: POSProfileDao,
    private val openingDao: POSOpeningEntryDao,
    private val openingEntryLinkDao: POSOpeningEntryLinkDao,
    private val closingDao: POSClosingEntryDao,
    private val companyDao: CompanyDao,
    private val cashboxDao: CashboxDao,
    private val userDao: UserDao,
    private val exchangeRatePreferences: ExchangeRatePreferences,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val openingEntrySyncRepository: OpeningEntrySyncRepository,
    private val closingEntrySyncRepository: ClosingEntrySyncRepository,
    private val posOpeningRepository: PosOpeningRepository,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val generalPreferences: GeneralPreferences,
    private val currencySettingsRepository: CurrencySettingsRepository,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor,
    private val bootstrapContextPreferences: BootstrapContextPreferences
) {
    private val allowRemoteReadsFromUi = false

    //Contexto actual del POS cargado en memoria
    private var currentContext: POSContext? = null
    private val _contextFlow: MutableStateFlow<POSContext?> = MutableStateFlow(null)
    val contextFlow = _contextFlow.asStateFlow()

    private val _cashboxState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val cashboxState = _cashboxState.asStateFlow()

    suspend fun initializeContext(): POSContext? = withContext(Dispatchers.IO) {
        currencySettingsRepository.loadCached()
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        val canValidateRemoteShift = isOnline && sessionRefresher.ensureValidSession()
        val user = resolveCurrentUser(canUseRemote = canValidateRemoteShift) ?: return@withContext null
        val serverUser = resolveServerUserId(user)
        openingEntrySyncRepository.repairActiveOpenings()
        val company = companyDao.getCompanyInfo()
        val activeProfile = profileDao.getActiveProfile()
        val activeCashbox = activeProfile?.let {
            findActiveCashboxForProfile(serverUser, it.profileName)
        }
        val resolvedCashbox = activeCashbox ?: findActiveCashboxForUser(serverUser)
        resolvedCashbox?.cashbox?.openingEntryId?.let { openingId ->
            bootstrapContextPreferences.update(
                profileName = resolvedCashbox.cashbox.posProfile,
                posOpeningEntry = openingId
            )
        }
        var bootstrapWithoutOpenShift = false

        // Si bootstrap trae open_shift, ese POE remoto manda: se adopta/restaura tal cual.
        if (canValidateRemoteShift) {
            val bootstrapShiftResult = runCatching {
                api.getBootstrapShiftSnapshot()
            }
                .onFailure {
                    AppLogger.warn("initializeContext: bootstrap open_shift lookup failed", it)
                }
            val bootstrapSnapshot = bootstrapShiftResult.getOrNull()
            val bootstrapShift = bootstrapSnapshot?.openShift
            val bootstrapClosing = bootstrapSnapshot?.posClosingEntry
            bootstrapWithoutOpenShift = bootstrapShiftResult.isSuccess && bootstrapShift == null
            if (bootstrapShift != null) {
                if (!isShiftOwnedByUser(bootstrapShift.user, user)) {
                    AppLogger.warn(
                        "initializeContext: bootstrap open_shift ignored for mismatched user. " +
                                "remoteUser=${bootstrapShift.user} localUser=$serverUser"
                    )
                } else {
                    val bootstrapProfile = runCatching {
                        profileDao.getPOSProfile(bootstrapShift.posProfile)
                    }.getOrNull()
                    if (bootstrapProfile != null) {
                        val existingForBootstrap =
                            findActiveCashboxForProfile(serverUser, bootstrapProfile.profileName)
                        val syncedCashbox = if (existingForBootstrap != null) {
                            runCatching {
                                adoptRemoteOpening(
                                    existingForBootstrap,
                                    bootstrapShift.name
                                )
                            }
                                .onFailure {
                                    AppLogger.warn(
                                        "initializeContext: adopt bootstrap open_shift failed",
                                        it
                                    )
                                }
                            findActiveCashboxForProfile(serverUser, bootstrapProfile.profileName)
                                ?: existingForBootstrap
                        } else {
                            val restoredId = runCatching {
                                restoreRemoteOpening(
                                    bootstrapProfile,
                                    bootstrapShift.name,
                                    serverUser
                                )
                            }.onFailure {
                                AppLogger.warn(
                                    "initializeContext: restore bootstrap open_shift failed",
                                    it
                                )
                            }.getOrNull()
                            restoredId?.let { restored ->
                                findActiveCashboxForProfile(serverUser, bootstrapProfile.profileName)
                                    ?: CashboxWithDetails(
                                        cashbox = CashboxEntity(
                                            localId = restored,
                                            openingEntryId = bootstrapShift.name,
                                            posProfile = bootstrapProfile.profileName,
                                            company = bootstrapProfile.company,
                                            periodStartDate = bootstrapShift.periodStartDate,
                                            user = bootstrapShift.user ?: serverUser,
                                            status = true,
                                            pendingSync = false
                                        ),
                                        details = emptyList()
                                    )
                            }
                        }
                        if (syncedCashbox != null) {
                            profileDao.updateProfileState(
                                user.username,
                                bootstrapProfile.profileName,
                                true
                            )
                            _cashboxState.update { true }
                            currentContext = buildContextFrom(
                                user = user,
                                profile = bootstrapProfile,
                                cashboxId = syncedCashbox.cashbox.localId,
                                company = company
                            )
                            _contextFlow.value = currentContext
                            updateBootstrapContext(serverUser, currentContext)
                            return@withContext currentContext
                        }
                    }
                }
            }
            if (bootstrapShift == null && bootstrapClosing != null) {
                AppLogger.warn(
                    "initializeContext: bootstrap returned closed shift ${bootstrapClosing.name}; " +
                            "closing local cashbox(es)"
                )
                applyBootstrapClosingForUser(
                    user = user,
                    closing = bootstrapClosing,
                    preferred = resolvedCashbox
                )
                return@withContext null
            }
        }

        if (bootstrapWithoutOpenShift && resolvedCashbox != null) {
            AppLogger.warn(
                "initializeContext: sync.bootstrap returned open_shift=null, closing local cashbox(es)"
            )
            forceCloseLocalCashboxesForUser(user, preferred = resolvedCashbox)
            return@withContext null
        }

        if (resolvedCashbox != null) {
            val profile = when {
                activeProfile != null && activeCashbox != null -> activeProfile
                else -> profileDao.getPOSProfile(resolvedCashbox.cashbox.posProfile)
            }
            profileDao.updateProfileState(user.username, profile.profileName, true)
            _cashboxState.update { true }
            currentContext = buildContextFrom(
                user = user,
                profile = profile,
                cashboxId = resolvedCashbox.cashbox.localId,
                company = company
            )
            _contextFlow.value = currentContext
            updateBootstrapContext(serverUser, currentContext)
            return@withContext currentContext
        }
        if (!canValidateRemoteShift) {
            _cashboxState.update { false }
            return@withContext null
        }

        runCatching { openingEntrySyncRepository.pushPending() }
            .onFailure { AppLogger.warn("initializeContext: sync openings failed", it) }
        runCatching { closingEntrySyncRepository.reconcileRemoteClosingsForActiveCashboxes() }
            .onFailure { AppLogger.warn("initializeContext: reconcile remote closings failed", it) }

        val bootstrapShift = runCatching { api.getBootstrapOpenShift() }
            .onFailure { AppLogger.warn("initializeContext: bootstrap open_shift after sync failed", it) }
            .getOrNull()
        if (bootstrapShift != null) {
            if (!isShiftOwnedByUser(bootstrapShift.user, user)) {
                AppLogger.warn(
                    "initializeContext: bootstrap open_shift skipped after sync for mismatched user. " +
                            "remoteUser=${bootstrapShift.user} localUser=$serverUser"
                )
            } else {
                val bootstrapProfile = runCatching {
                    profileDao.getPOSProfile(bootstrapShift.posProfile)
                }.getOrNull()
                if (bootstrapProfile != null) {
                    val restored = runCatching {
                        restoreRemoteOpening(bootstrapProfile, bootstrapShift.name, serverUser)
                    }.getOrNull()
                    if (restored != null) {
                        profileDao.updateProfileState(
                            user.username,
                            bootstrapProfile.profileName,
                            true
                        )
                        _cashboxState.update { true }
                        currentContext = buildContextFrom(
                            user = user,
                            profile = bootstrapProfile,
                            cashboxId = restored,
                            company = company
                        )
                        _contextFlow.value = currentContext
                        updateBootstrapContext(serverUser, currentContext)
                        return@withContext currentContext
                    }
                }
            }
        }
        if (activeProfile != null) {
            val remoteOpen = resolveRemoteOpenSession(user, activeProfile.profileName)
            if (remoteOpen != null) {
                val restored = runCatching {
                    restoreRemoteOpening(activeProfile, remoteOpen.name, serverUser)
                }.getOrNull()
                if (restored != null) {
                    val restoredCashbox =
                        findActiveCashboxForProfile(serverUser, activeProfile.profileName)
                    if (restoredCashbox != null) {
                        _cashboxState.update { true }
                        currentContext = buildContextFrom(
                            user = user,
                            profile = activeProfile,
                            cashboxId = restoredCashbox.cashbox.localId,
                            company = company
                        )
                        _contextFlow.value = currentContext
                        updateBootstrapContext(serverUser, currentContext)
                        return@withContext currentContext
                    }
                }
            }
        }
        _cashboxState.update { false }
        return@withContext null
    }

    suspend fun openCashBox(
        entry: POSProfileSimpleBO,
        amounts: List<PaymentModeWithAmount>
    ): POSContext? = withContext(Dispatchers.IO) {
        val bootstrapContext = bootstrapContextPreferences.load()
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        if (isOnline && !sessionRefresher.ensureValidSession()) {
            AppLogger.warn("openCashBox: invalid session, aborting")
            return@withContext null
        }
        val user = resolveCurrentUser(canUseRemote = isOnline) ?: return@withContext null
        val serverUser = resolveServerUserId(user)
        val existing = findActiveCashboxForProfile(serverUser, entry.name)
        if (existing != null) {
            if (isOnline && allowRemoteReadsFromUi) {
                val remoteOpen = resolveRemoteOpenSession(user, entry.name)
                if (remoteOpen != null) {
                    adoptRemoteOpening(existing, remoteOpen.name)
                }
            }
            val remoteName = openingEntryLinkDao.getRemoteOpeningEntryName(existing.cashbox.localId)
            val needsSync = remoteName.isNullOrBlank() ||
                    existing.cashbox.openingEntryId?.startsWith("LOCAL-", ignoreCase = true) == true
            if (needsSync) {
                runCatching { openingEntrySyncRepository.pushPending() }
                    .onFailure { AppLogger.warn("openCashBox: sync existing opening failed", it) }
            }

            profileDao.updateProfileState(user.username, entry.name, true)
            val company = companyDao.getCompanyInfo()
            val profile = profileDao.getPOSProfile(entry.name)
            val exchangeRate = resolveExchangeRate(profile.currency)
            val allowedCurrencies =
                resolveSupportedCurrencies(profile.profileName, profile.currency)
            val paymentModes = resolvePaymentModes(profile.profileName)

            _cashboxState.update { true }
            currentContext = POSContext(
                username = user.username ?: user.name,
                profileName = entry.name,
                company = company?.companyName ?: profile.company,
                companyCurrency = company?.defaultCurrency ?: profile.currency,
                allowNegativeStock = generalPreferences.allowNegativeStock.firstOrNull() == true,
                warehouse = profile.warehouse,
                route = profile.route,
                territory = profile.route,
                priceList = profile.sellingPriceList,
                isCashBoxOpen = true,
                cashboxId = existing.cashbox.localId,
                incomeAccount = profile.incomeAccount,
                expenseAccount = profile.expenseAccount,
                branch = profile.branch,
                applyDiscountOn = profile.applyDiscountOn,
                currency = profile.currency,
                exchangeRate = exchangeRate,
                allowedCurrencies = allowedCurrencies,
                paymentModes = paymentModes,
                cashier = user.toBO(),
                partyAccountCurrency = company?.defaultCurrency ?: profile.currency,
                monthlySalesTarget = bootstrapContext.monthlySalesTarget,
                defaultReceivableAccount = company?.defaultReceivableAccount,
                defaultReceivableAccountCurrency = company?.defaultReceivableAccountCurrency,
            )
            _contextFlow.value = currentContext
            updateBootstrapContext(serverUser, currentContext)
            return@withContext currentContext
        }
        if (isOnline && allowRemoteReadsFromUi) {
            val remoteOpen = resolveRemoteOpenSession(user, entry.name)
            if (remoteOpen != null) {
                val restored = runCatching {
                    restoreRemoteOpening(
                        profileDao.getPOSProfile(entry.name),
                        remoteOpen.name,
                        serverUser,
                        amounts
                    )
                }.getOrNull()
                if (restored != null) {
                    _cashboxState.update { true }
                    currentContext = buildContextFrom(
                        user = user,
                        profile = profileDao.getPOSProfile(entry.name),
                        cashboxId = restored,
                        company = companyDao.getCompanyInfo()
                    )
                    _contextFlow.value = currentContext
                    updateBootstrapContext(serverUser, currentContext)
                    return@withContext currentContext
                }
            }
        }
        val now = getTimeMillis()
        val periodStart = now.toErpDateTime()
        val openingEntryName = buildLocalOpeningEntryId(entry.name, serverUser, now)
        val openingEntry = POSOpeningEntryEntity(
            name = openingEntryName,
            posProfile = entry.name,
            company = entry.company,
            periodStartDate = periodStart,
            postingDate = periodStart,
            user = serverUser,
            pendingSync = true
        )
        openingDao.insert(openingEntry)

        val cashbox = CashboxEntity(
            localId = 0,
            posProfile = entry.name,
            company = entry.company,
            periodStartDate = periodStart,
            user = serverUser,
            status = true,
            pendingSync = true,
            openingEntryId = openingEntryName,
        )
        val balanceDetails = amounts.map {
            BalanceDetailsEntity(
                cashboxId = 0,
                posOpeningEntry = openingEntryName,
                modeOfPayment = it.mode.modeOfPayment,
                openingAmount = it.amount,
                closingAmount = null
            )
        }
        val cashboxId = cashboxDao.insert(cashbox, balanceDetails)
        openingEntryLinkDao.insert(
            POSOpeningEntryLinkEntity(
                cashboxId = cashboxId,
                localOpeningEntryName = openingEntryName,
                pendingSync = true
            )
        )
        runCatching { openingEntrySyncRepository.pushPending() }
            .onFailure { AppLogger.warn("openCashBox: immediate sync failed", it) }

        profileDao.updateProfileState(user.username, entry.name, true)
        val company = companyDao.getCompanyInfo()
        val profile = profileDao.getActiveProfile() ?: error("Profile not found")

        _cashboxState.update { true }

        val exchangeRate = resolveExchangeRate(profile.currency)
        val allowedCurrencies = resolveSupportedCurrencies(profile.profileName, profile.currency)
        val paymentModes = resolvePaymentModes(profile.profileName)
        currentContext = POSContext(
            username = user.username ?: user.name,
            profileName = entry.name,
            company = company?.companyName ?: profile.company,
            companyCurrency = company?.defaultCurrency ?: profile.currency,
            allowNegativeStock = generalPreferences.allowNegativeStock.firstOrNull() == true,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = true,
            cashboxId = cashboxId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            applyDiscountOn = profile.applyDiscountOn,
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes,
            cashier = user.toBO(),
            partyAccountCurrency = profile.currency,
            monthlySalesTarget = bootstrapContext.monthlySalesTarget,
            defaultReceivableAccount = company?.defaultReceivableAccount,
            defaultReceivableAccountCurrency = company?.defaultReceivableAccountCurrency,
        )
        _contextFlow.value = currentContext
        updateBootstrapContext(serverUser, currentContext)
        currentContext!!
    }

    private suspend fun updateBootstrapContext(
        serverUserId: String,
        context: POSContext?
    ) {
        val ctx = context ?: return
        val opening = cashboxDao.getActiveEntry(serverUserId, ctx.profileName)
            .firstOrNull()
            ?.cashbox
            ?.openingEntryId
        bootstrapContextPreferences.update(
            profileName = ctx.profileName,
            posOpeningEntry = opening,
            monthlySalesTarget = ctx.monthlySalesTarget
        )
    }

    private suspend fun adoptRemoteOpening(
        existing: CashboxWithDetails,
        remoteName: String
    ) {
        val localName = existing.cashbox.openingEntryId
        if (localName != null && localName.equals(remoteName, ignoreCase = true)) return
        val detail = runCatching { api.getPOSOpeningEntry(remoteName) }.getOrNull()
        val periodStart = detail?.periodStartDate ?: existing.cashbox.periodStartDate
        val postingDate = detail?.postingDate ?: periodStart
        val openingEntry = POSOpeningEntryEntity(
            name = remoteName,
            posProfile = existing.cashbox.posProfile,
            company = detail?.company ?: existing.cashbox.company,
            periodStartDate = periodStart,
            postingDate = postingDate,
            user = detail?.user ?: existing.cashbox.user,
            pendingSync = false
        )
        openingDao.insert(openingEntry)
        cashboxDao.updateOpeningEntryId(existing.cashbox.localId, remoteName)
        detail?.user
            ?.takeIf { it.isNotBlank() && !it.equals(existing.cashbox.user, ignoreCase = true) }
            ?.let { cashboxDao.updateUser(existing.cashbox.localId, it) }
        val link = openingEntryLinkDao.getByCashboxId(existing.cashbox.localId)
        if (link == null) {
            openingEntryLinkDao.insert(
                POSOpeningEntryLinkEntity(
                    cashboxId = existing.cashbox.localId,
                    localOpeningEntryName = localName ?: remoteName,
                    remoteOpeningEntryName = remoteName,
                    pendingSync = false
                )
            )
        } else {
            openingEntryLinkDao.updateRemoteName(link.id, remoteName)
            openingEntryLinkDao.markSynced(link.id, remoteName)
        }
        if (!localName.isNullOrBlank() && !localName.equals(remoteName, ignoreCase = true)) {
            cashboxDao.updateBalanceDetailsOpeningEntry(localName, remoteName)
            salesInvoiceDao.updateInvoicesOpeningEntry(localName, remoteName)
            salesInvoiceDao.updatePaymentsOpeningEntry(localName, remoteName)
        }
        if (existing.cashbox.pendingSync) {
            cashboxDao.updatePendingSync(existing.cashbox.localId, false)
        }
    }

    private suspend fun restoreRemoteOpening(
        profile: com.erpnext.pos.localSource.entities.POSProfileEntity,
        remoteName: String,
        userId: String,
        fallbackAmounts: List<PaymentModeWithAmount> = emptyList()
    ): Long {
        val detail = runCatching { api.getPOSOpeningEntry(remoteName) }.getOrNull()
        val resolvedUser = detail?.user?.takeIf { it.isNotBlank() } ?: userId
        val periodStart = detail?.periodStartDate ?: getTimeMillis().toErpDateTime()
        val postingDate = detail?.postingDate ?: periodStart
        val openingEntry = POSOpeningEntryEntity(
            name = remoteName,
            posProfile = profile.profileName,
            company = detail?.company ?: profile.company,
            periodStartDate = periodStart,
            postingDate = postingDate,
            user = resolvedUser,
            pendingSync = false
        )
        openingDao.insert(openingEntry)
        val cashbox = CashboxEntity(
            localId = 0,
            posProfile = profile.profileName,
            company = detail?.company ?: profile.company,
            periodStartDate = periodStart,
            user = resolvedUser,
            status = true,
            pendingSync = false,
            openingEntryId = remoteName,
        )
        val balanceDetails = if (!detail?.balanceDetails.isNullOrEmpty()) {
            detail.balanceDetails.map {
                BalanceDetailsEntity(
                    cashboxId = 0,
                    posOpeningEntry = remoteName,
                    modeOfPayment = it.modeOfPayment,
                    openingAmount = it.openingAmount,
                    closingAmount = null
                )
            }
        } else {
            fallbackAmounts.map {
                BalanceDetailsEntity(
                    cashboxId = 0,
                    posOpeningEntry = remoteName,
                    modeOfPayment = it.mode.modeOfPayment,
                    openingAmount = it.amount,
                    closingAmount = null
                )
            }
        }
        val cashboxId = cashboxDao.insert(cashbox, balanceDetails)
        openingEntryLinkDao.insert(
            POSOpeningEntryLinkEntity(
                cashboxId = cashboxId,
                localOpeningEntryName = remoteName,
                remoteOpeningEntryName = remoteName,
                pendingSync = false
            )
        )
        return cashboxId
    }

    private fun isShiftOwnedByUser(shiftUser: String?, user: UserEntity): Boolean {
        val remote = shiftUser?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return true
        val canonical = resolveServerUserId(user).trim().lowercase()
        return remote == canonical
    }

    private suspend fun resolveCurrentUser(canUseRemote: Boolean): UserEntity? {
        val local = userDao.getUserInfo()
        if (!canUseRemote) return local
        val remote = runCatching {
            api.getUserInfo().toEntity()
        }.onFailure {
            AppLogger.warn("resolveCurrentUser: getUserInfo failed, using local cache", it)
        }.getOrNull()
        if (remote != null) {
            userDao.addUser(remote)
            return remote
        }
        return local
    }

    private fun resolveServerUserId(user: UserEntity): String {
        return user.name.trim()
            .ifBlank { user.username?.trim().orEmpty() }
            .ifBlank { user.email.trim() }
    }

    private suspend fun findActiveCashboxForProfile(
        userId: String,
        profileName: String
    ): CashboxWithDetails? {
        if (userId.isBlank()) return null
        return cashboxDao.getActiveEntry(userId, profileName).firstOrNull()
    }

    private suspend fun findActiveCashboxForUser(
        userId: String
    ): CashboxWithDetails? {
        if (userId.isBlank()) return null
        return cashboxDao.getActiveEntryForUser(userId)
    }

    private suspend fun forceCloseLocalCashboxesForUser(
        user: UserEntity,
        preferred: CashboxWithDetails? = null
    ) {
        val ownedByUser = listOf(
            resolveServerUserId(user),
            user.name,
            user.username,
            user.email
        ).mapNotNull { candidate ->
            candidate?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        }.toSet()
        val active = cashboxDao.getActiveCashboxes()
            .filter { wrapper ->
                wrapper.cashbox.user.trim().lowercase().let { owner ->
                    owner.isNotBlank() && owner in ownedByUser
                }
            }
            .toMutableList()
        if (preferred != null && active.none { it.cashbox.localId == preferred.cashbox.localId }) {
            active += preferred
        }
        val endDate = getTimeMillis().toErpDateTime()
        active.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            cashboxDao.updateStatus(
                localId = cashbox.localId,
                status = false,
                pceId = "",
                endDate = endDate,
                pendingSync = false
            )
            cashbox.openingEntryId
                ?.takeIf { it.isNotBlank() }
                ?.let { openingDao.markSynced(it) }
            openingEntryLinkDao.clearPendingSyncByCashboxId(cashbox.localId)
            runCatching {
                profileDao.updateProfileState(user.username, cashbox.posProfile, false)
            }.onFailure {
                AppLogger.warn(
                    "forceCloseLocalCashboxesForUser: update profile state failed for ${cashbox.posProfile}",
                    it
                )
            }
        }
        currentContext = null
        _contextFlow.value = null
        _cashboxState.update { false }
    }

    private suspend fun applyBootstrapClosingForUser(
        user: UserEntity,
        closing: BootstrapClosingEntryDto,
        preferred: CashboxWithDetails? = null
    ) {
        val closingProfile = closing.posProfile?.trim().orEmpty()
        AppLogger.info(
            "applyBootstrapClosingForUser: closing=${closing.name} profile=$closingProfile " +
                    "opening=${closing.posOpeningEntry} user=${closing.user}"
        )
        val ownedByUser = listOf(
            resolveServerUserId(user),
            user.name,
            user.username,
            user.email
        ).mapNotNull { candidate ->
            candidate?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        }.toSet()
        val activePool = cashboxDao.getActiveCashboxes()
        val allActive = activePool
            .filter { wrapper ->
                wrapper.cashbox.user.trim().lowercase().let { owner ->
                    owner.isNotBlank() && owner in ownedByUser
                }
            }
            .toMutableList()
        val openingFromBootstrap = closing.posOpeningEntry?.trim()?.takeIf { it.isNotBlank() }
        if (!openingFromBootstrap.isNullOrBlank()) {
            val byOpening = cashboxDao.getByOpeningEntry(openingFromBootstrap)
            if (byOpening != null && allActive.none { it.cashbox.localId == byOpening.cashbox.localId }) {
                allActive += byOpening
            }
        }
        if (allActive.isEmpty() && closingProfile.isNotBlank()) {
            allActive += activePool.filter { wrapper ->
                wrapper.cashbox.posProfile.equals(closingProfile, ignoreCase = true)
            }
        }
        if (preferred != null && allActive.none { it.cashbox.localId == preferred.cashbox.localId }) {
            allActive += preferred
        }
        val endDate = closing.periodEndDate
            ?: closing.postingDate
            ?: getTimeMillis().toErpDateTime()
        if (allActive.isEmpty() && !openingFromBootstrap.isNullOrBlank()) {
            val placeholderCashbox = CashboxEntity(
                localId = 0,
                openingEntryId = openingFromBootstrap,
                closingEntryId = closing.name,
                posProfile = closingProfile.ifBlank { preferred?.cashbox?.posProfile.orEmpty() },
                company = closing.company?.takeIf { it.isNotBlank() }
                    ?: preferred?.cashbox?.company.orEmpty(),
                periodStartDate = closing.periodStartDate ?: endDate,
                periodEndDate = endDate,
                user = closing.user?.takeIf { it.isNotBlank() } ?: resolveServerUserId(user),
                status = false,
                pendingSync = false
            )
            ensureOpeningEntryForClosing(placeholderCashbox, openingFromBootstrap, closing, user)
            openingDao.markSynced(openingFromBootstrap)
            val closingAmount = closing.paymentReconciliation.sumOf { it.closingAmount }
            closingDao.insert(
                com.erpnext.pos.localSource.entities.POSClosingEntryEntity(
                    name = closing.name,
                    posProfile = closingProfile.ifBlank { placeholderCashbox.posProfile },
                    posOpeningEntry = openingFromBootstrap,
                    user = placeholderCashbox.user,
                    periodStartDate = closing.periodStartDate ?: placeholderCashbox.periodStartDate,
                    periodEndDate = endDate,
                    closingAmount = closingAmount,
                    pendingSync = false
                )
            )
            AppLogger.info(
                "applyBootstrapClosingForUser: persisted standalone closing=${closing.name} opening=$openingFromBootstrap"
            )
        }
        if (allActive.isEmpty()) {
            AppLogger.warn(
                "applyBootstrapClosingForUser: no matching cashbox found; context cleared only"
            )
            currentContext = null
            _contextFlow.value = null
            _cashboxState.update { false }
            return
        }

        val primary = allActive.firstOrNull { wrapper ->
            closingProfile.isNotBlank() &&
                    wrapper.cashbox.posProfile.equals(closingProfile, ignoreCase = true)
        } ?: preferred ?: allActive.first()
        val openingName = openingFromBootstrap
            ?: primary.cashbox.openingEntryId?.trim()?.takeIf { it.isNotBlank() }

        if (!openingName.isNullOrBlank()) {
            ensureOpeningEntryForClosing(primary.cashbox, openingName, closing, user)
            val previousOpening = primary.cashbox.openingEntryId?.trim()
            if (!previousOpening.isNullOrBlank() && !previousOpening.equals(openingName, ignoreCase = true)) {
                cashboxDao.updateBalanceDetailsOpeningEntry(previousOpening, openingName)
                salesInvoiceDao.updateInvoicesOpeningEntry(previousOpening, openingName)
                salesInvoiceDao.updatePaymentsOpeningEntry(previousOpening, openingName)
            }
            cashboxDao.updateOpeningEntryId(primary.cashbox.localId, openingName)
            openingDao.markSynced(openingName)
            upsertClosingLinkForCashbox(
                cashbox = primary.cashbox,
                closingEntryName = closing.name,
                localOpeningEntryName = openingName,
                remoteOpeningEntryName = openingName,
                pendingSync = false
            )
            val closingAmount = closing.paymentReconciliation.sumOf { it.closingAmount }
            closingDao.insert(
                com.erpnext.pos.localSource.entities.POSClosingEntryEntity(
                    name = closing.name,
                    posProfile = closing.posProfile?.takeIf { it.isNotBlank() } ?: primary.cashbox.posProfile,
                    posOpeningEntry = openingName,
                    user = closing.user?.takeIf { it.isNotBlank() } ?: primary.cashbox.user,
                    periodStartDate = closing.periodStartDate
                        ?: primary.cashbox.periodStartDate,
                    periodEndDate = endDate,
                    closingAmount = closingAmount,
                    pendingSync = false
                )
            )
        }

        allActive.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            val updatedRows = cashboxDao.updateStatus(
                localId = cashbox.localId,
                status = false,
                pceId = closing.name,
                endDate = endDate,
                pendingSync = false
            )
            upsertClosingLinkForCashbox(
                cashbox = cashbox,
                closingEntryName = closing.name,
                localOpeningEntryName = cashbox.openingEntryId,
                remoteOpeningEntryName = openingName,
                pendingSync = false
            )
            val linkRows = openingEntryLinkDao.clearPendingSyncByCashboxId(cashbox.localId)
            cashbox.openingEntryId
                ?.takeIf { it.isNotBlank() }
                ?.let { openingDao.markSynced(it) }
            if (!openingName.isNullOrBlank() && cashbox.localId == primary.cashbox.localId) {
                cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, closing.name)
            } else if (cashbox.closingEntryId?.isNotBlank() == true) {
                cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, closing.name)
            }
            runCatching {
                profileDao.updateProfileState(user.username, cashbox.posProfile, false)
            }.onFailure {
                AppLogger.warn(
                    "applyBootstrapClosingForUser: update profile state failed for ${cashbox.posProfile}",
                    it
                )
            }
            AppLogger.info(
                "applyBootstrapClosingForUser: cashbox=${cashbox.localId} updatedRows=$updatedRows " +
                        "linkRows=$linkRows " +
                        "opening=${cashbox.openingEntryId} closing=${closing.name}"
            )
        }

        currentContext = null
        _contextFlow.value = null
        _cashboxState.update { false }
    }

    private suspend fun ensureOpeningEntryForClosing(
        cashbox: CashboxEntity,
        openingName: String,
        closing: BootstrapClosingEntryDto,
        user: UserEntity
    ) {
        val existing = openingDao.getByName(openingName)
        if (existing != null) return
        openingDao.insert(
            POSOpeningEntryEntity(
                name = openingName,
                posProfile = closing.posProfile?.takeIf { it.isNotBlank() } ?: cashbox.posProfile,
                company = closing.company?.takeIf { it.isNotBlank() } ?: cashbox.company,
                periodStartDate = closing.periodStartDate ?: cashbox.periodStartDate,
                postingDate = closing.periodStartDate ?: cashbox.periodStartDate,
                user = closing.user?.takeIf { it.isNotBlank() } ?: resolveServerUserId(user),
                pendingSync = false
            )
        )
    }

    private suspend fun upsertClosingLinkForCashbox(
        cashbox: CashboxEntity,
        closingEntryName: String,
        localOpeningEntryName: String?,
        remoteOpeningEntryName: String?,
        pendingSync: Boolean
    ) {
        val existing = openingEntryLinkDao.getByCashboxId(cashbox.localId)
        if (existing != null) {
            val remoteOpening = remoteOpeningEntryName?.trim()?.takeIf { it.isNotBlank() }
            if (!remoteOpening.isNullOrBlank() &&
                !remoteOpening.equals(existing.remoteOpeningEntryName, ignoreCase = true)
            ) {
                openingEntryLinkDao.updateRemoteName(existing.id, remoteOpening)
                if (!pendingSync) {
                    openingEntryLinkDao.markSynced(existing.id, remoteOpening)
                }
            } else if (!remoteOpening.isNullOrBlank() && !pendingSync && existing.pendingSync) {
                openingEntryLinkDao.markSynced(existing.id, remoteOpening)
            }
            if (!closingEntryName.equals(existing.remoteClosingEntryName, ignoreCase = true)) {
                openingEntryLinkDao.updateRemoteClosingName(existing.id, closingEntryName)
            }
            return
        }

        val localOpening = localOpeningEntryName?.trim()?.takeIf { it.isNotBlank() }
            ?: remoteOpeningEntryName?.trim()?.takeIf { it.isNotBlank() }
        if (localOpening.isNullOrBlank()) {
            AppLogger.warn(
                "upsertClosingLinkForCashbox: missing opening entry for cashbox ${cashbox.localId}"
            )
            return
        }

        if (openingDao.getByName(localOpening) == null) {
            openingDao.insert(
                POSOpeningEntryEntity(
                    name = localOpening,
                    posProfile = cashbox.posProfile,
                    company = cashbox.company,
                    periodStartDate = cashbox.periodStartDate,
                    postingDate = cashbox.periodStartDate,
                    user = cashbox.user,
                    pendingSync = pendingSync
                )
            )
        }

        val remoteOpening = remoteOpeningEntryName?.trim()?.takeIf { it.isNotBlank() }
            ?: localOpening.takeIf { !it.startsWith("LOCAL-", ignoreCase = true) }
        openingEntryLinkDao.insert(
            POSOpeningEntryLinkEntity(
                cashboxId = cashbox.localId,
                localOpeningEntryName = localOpening,
                remoteOpeningEntryName = remoteOpening,
                remoteClosingEntryName = closingEntryName,
                pendingSync = pendingSync
            )
        )
    }

    private suspend fun buildContextFrom(
        user: UserEntity,
        profile: com.erpnext.pos.localSource.entities.POSProfileEntity,
        cashboxId: Long,
        company: CompanyEntity?
    ): POSContext {
        val exchangeRate = resolveExchangeRate(profile.currency)
        val allowedCurrencies = resolveSupportedCurrencies(profile.profileName, profile.currency)
        val paymentModes = resolvePaymentModes(profile.profileName)
        return POSContext(
            cashier = user.toBO(),
            username = user.username ?: user.name,
            profileName = profile.profileName,
            company = company?.companyName ?: profile.company,
            companyCurrency = company?.defaultCurrency ?: profile.currency,
            allowNegativeStock = generalPreferences.allowNegativeStock.firstOrNull() == true,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = true,
            cashboxId = cashboxId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            applyDiscountOn = profile.applyDiscountOn,
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes,
            partyAccountCurrency = company?.defaultCurrency ?: profile.currency,
            monthlySalesTarget = bootstrapContextPreferences.load().monthlySalesTarget,
            defaultReceivableAccount = company?.defaultReceivableAccount,
            defaultReceivableAccountCurrency = company?.defaultReceivableAccountCurrency
        )
    }

    private suspend fun resolveRemoteOpenSession(
        user: UserEntity,
        profileName: String
    ): POSOpeningEntrySummaryDto? {
        val userId = resolveServerUserId(user).trim()
        if (userId.isBlank()) return null
        return runCatching {
            posOpeningRepository.getOpenSession(userId, profileName)
        }.onFailure {
            AppLogger.warn(
                "resolveRemoteOpenSession failed for user=$userId profile=$profileName",
                it
            )
        }.getOrNull()
    }

    suspend fun closeCashBox(): POSContext? = withContext(Dispatchers.IO) {
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        val canUseRemote = isOnline && sessionRefresher.ensureValidSession()
        if (isOnline && !canUseRemote) {
            AppLogger.warn("closeCashBox: invalid session, falling back to local close")
        }
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) return@withContext null
        val user = resolveCurrentUser(canUseRemote = canUseRemote) ?: return@withContext null

        val entry = findActiveCashboxForProfile(
            resolveServerUserId(user),
            ctx.profileName
        )
            ?: return@withContext null
        val endMillis = getTimeMillis()
        val startMillis = parseErpDateTimeToEpochMillis(entry.cashbox.periodStartDate)
            ?: endMillis
        var remoteOpeningEntryId =
            openingEntryLinkDao.getRemoteOpeningEntryName(entry.cashbox.localId)
        if (canUseRemote && remoteOpeningEntryId.isNullOrBlank()) {
            runCatching { openingEntrySyncRepository.pushPending() }
                .onFailure { AppLogger.warn("closeCashBox: sync opening failed", it) }
            remoteOpeningEntryId =
                openingEntryLinkDao.getRemoteOpeningEntryName(entry.cashbox.localId)
        }
        if (canUseRemote && remoteOpeningEntryId.isNullOrBlank()) {
            val remoteFromBootstrap = runCatching { api.getBootstrapOpenShift()?.name }
                .onFailure { AppLogger.warn("closeCashBox: bootstrap POE lookup failed", it) }
                .getOrNull()
            if (!remoteFromBootstrap.isNullOrBlank()) {
                remoteOpeningEntryId = remoteFromBootstrap
                cashboxDao.updateOpeningEntryId(entry.cashbox.localId, remoteFromBootstrap)
                val localOpening = entry.cashbox.openingEntryId?.takeIf { it.isNotBlank() }
                if (!localOpening.isNullOrBlank() && localOpening != remoteFromBootstrap) {
                    cashboxDao.updateBalanceDetailsOpeningEntry(localOpening, remoteFromBootstrap)
                    salesInvoiceDao.updateInvoicesOpeningEntry(localOpening, remoteFromBootstrap)
                    salesInvoiceDao.updatePaymentsOpeningEntry(localOpening, remoteFromBootstrap)
                }
                AppLogger.info("closeCashBox: resolved remote POE from bootstrap -> $remoteFromBootstrap")
            }
        }
        val localOpeningEntryId = entry.cashbox.openingEntryId?.takeIf { it.isNotBlank() }
        val openingEntryId = remoteOpeningEntryId ?: localOpeningEntryId ?: return@withContext null
        val shiftInvoices = buildList {
            if (openingEntryId.isNotBlank()) {
                addAll(salesInvoiceDao.getInvoicesForOpeningEntry(openingEntryId))
            }
            if (isEmpty() && localOpeningEntryId != null && localOpeningEntryId != openingEntryId) {
                addAll(salesInvoiceDao.getInvoicesForOpeningEntry(localOpeningEntryId))
            }
            if (isEmpty()) {
                addAll(
                    salesInvoiceDao.getInvoicesForShift(
                        profileId = ctx.profileName,
                        startMillis = startMillis,
                        endMillis = endMillis
                    )
                )
            }
        }
        val resolvedInvoices = if (canUseRemote && allowRemoteReadsFromUi) {
            reconcileRemoteInvoicesForClosing(
                openingEntryId = openingEntryId,
                posProfile = ctx.profileName,
                invoices = shiftInvoices
            )
        } else {
            shiftInvoices
        }
        resolvedInvoices.forEach { invoice ->
            val name = invoice.invoiceName?.trim().orEmpty()
            if (name.isBlank() || name.startsWith("LOCAL-", ignoreCase = true)) return@forEach
            if (invoice.posOpeningEntry != openingEntryId || invoice.profileId != ctx.profileName) {
                salesInvoiceDao.updateInvoiceOpeningAndProfile(
                    invoiceName = name,
                    posOpeningEntry = openingEntryId,
                    profileId = ctx.profileName
                )
                salesInvoiceDao.updatePaymentsOpeningForInvoice(name, openingEntryId)
            }
        }
        val paymentRows = buildList {
            if (openingEntryId.isNotBlank()) {
                addAll(salesInvoiceDao.getPaymentsForOpeningEntry(openingEntryId))
            }
            if (isEmpty() && localOpeningEntryId != null && localOpeningEntryId != openingEntryId) {
                addAll(salesInvoiceDao.getPaymentsForOpeningEntry(localOpeningEntryId))
            }
            if (isEmpty()) {
                addAll(salesInvoiceDao.getShiftPayments(ctx.profileName, startMillis, endMillis))
            }
        }
        val modeCurrency = runCatching {
            paymentMethodLocalRepository.getMethodsForProfile(ctx.profileName)
        }.getOrElse { emptyList() }
            .associate { method ->
                val currency = normalizeCurrency(method.currency ?: ctx.currency)
                method.mopName to currency
            }
        val paymentReconciliation = buildPaymentReconciliationSeeds(
            balanceDetails = entry.details,
            paymentRows = paymentRows,
            invoices = resolvedInvoices,
            modeCurrency = modeCurrency,
            posCurrency = normalizeCurrency(ctx.currency),
            rateResolver = { from, to -> exchangeRateRepository.getRate(from, to) }
        )
        val paidInvoiceNames = paymentRows.asSequence()
            .filter { (it.enteredAmount > 0.0001) || (it.amount > 0.0001) }
            .map { it.invoiceName.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val endDate = getTimeMillis().toErpDateTime()
        val dto = buildClosingEntryDto(
            cashbox = entry.cashbox,
            openingEntryId = openingEntryId,
            postingDate = endDate,
            periodEndDate = endDate,
            paymentReconciliation = paymentReconciliation,
            invoices = resolvedInvoices,
            paidInvoiceNames = paidInvoiceNames
        )
        if (!remoteOpeningEntryId.isNullOrBlank()) {
            val existing = openingDao.getByName(remoteOpeningEntryId)
            if (existing == null) {
                val local =
                    openingDao.getByName(localOpeningEntryId ?: "") ?: return@withContext null
                openingDao.insert(local.copy(name = remoteOpeningEntryId, pendingSync = false))
            }
        }
        val pce = if (canUseRemote && !remoteOpeningEntryId.isNullOrBlank()) {
            runCatching { api.closeCashbox(dto) }
                .onFailure { AppLogger.warn("closeCashBox: remote close failed", it) }
                .getOrNull()
        } else {
            null
        }
        val closingEntryId = pce?.name
            ?: buildLocalClosingEntryId(ctx.profileName, resolveServerUserId(user), endMillis)
        val pendingSync = pce == null

        cashboxDao.updateStatus(
            entry.cashbox.localId,
            status = false,
            closingEntryId,
            dto.periodEndDate,
            pendingSync = pendingSync
        )
        upsertClosingLinkForCashbox(
            cashbox = entry.cashbox,
            closingEntryName = closingEntryId,
            localOpeningEntryName = localOpeningEntryId ?: entry.cashbox.openingEntryId,
            remoteOpeningEntryName = remoteOpeningEntryId,
            pendingSync = remoteOpeningEntryId.isNullOrBlank()
        )
        profileDao.updateProfileState(ctx.username, ctx.profileName, false)
        closingDao.insert(dto.toEntity(closingEntryId, pendingSync = pendingSync))
        cashboxDao.updateBalanceDetailsClosingEntry(entry.cashbox.localId, closingEntryId)

        _cashboxState.update { false }
        currentContext = null
        _contextFlow.value = null
        currentContext = null
        currentContext
    }

    private suspend fun reconcileRemoteInvoicesForClosing(
        openingEntryId: String,
        posProfile: String,
        invoices: List<SalesInvoiceEntity>
    ): List<SalesInvoiceEntity> {
        if (invoices.isEmpty()) return invoices
        val resolved = mutableListOf<SalesInvoiceEntity>()
        invoices.forEach { invoice ->
            val name = invoice.invoiceName?.trim().orEmpty()
            if (name.isBlank() || name.startsWith("LOCAL-", ignoreCase = true)) return@forEach
            if (invoice.docstatus != 1) return@forEach
            val remote = runCatching { api.getSalesInvoiceByName(name) }.getOrNull()
            if (remote == null) {
                AppLogger.warn("closeCashBox: remote invoice missing for $name")
                return@forEach
            }
            val matches = remote.posOpeningEntry == openingEntryId &&
                    remote.posProfile == posProfile
            if (matches) {
                if (invoice.posOpeningEntry != openingEntryId || invoice.profileId != posProfile) {
                    salesInvoiceDao.updateInvoiceOpeningAndProfile(
                        invoiceName = name,
                        posOpeningEntry = openingEntryId,
                        profileId = posProfile
                    )
                    salesInvoiceDao.updatePaymentsOpeningForInvoice(name, openingEntryId)
                }
                resolved += invoice
                return@forEach
            }
            AppLogger.warn(
                "closeCashBox: remote invoice $name sin apertura/perfil esperado; set_value legacy removido."
            )
        }
        return resolved
    }

    fun getContext(): POSContext? = currentContext

    fun requireContext(): POSContext =
        currentContext ?: error("POS context not initialized. Call initializeContext() first.")

    fun clearContext() {
        currentContext = null
        _contextFlow.value = null
        _cashboxState.update { false }
    }

    fun activeCashboxStart(): Flow<String?> = flow {
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) {
            emit(null)
            return@flow
        }
        val user = resolveCurrentUser(canUseRemote = false)
        if (user == null) {
            emit(null)
            return@flow
        }
        val userId = resolveServerUserId(user)
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        emitAll(
            cashboxDao.getActiveEntry(userId, ctx.profileName)
                .map { it?.cashbox?.periodStartDate }
        )
    }

    fun activeOpeningEntryId(): Flow<String?> = flow {
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) {
            emit(null)
            return@flow
        }
        val user = resolveCurrentUser(canUseRemote = false)
        if (user == null) {
            emit(null)
            return@flow
        }
        val userId = resolveServerUserId(user)
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        emitAll(
            cashboxDao.getActiveEntry(userId, ctx.profileName)
                .map { it?.cashbox?.openingEntryId }
        )
    }

    suspend fun getActiveCashboxWithDetails(): CashboxWithDetails? = withContext(Dispatchers.IO) {
        val ctx = currentContext ?: initializeContext() ?: return@withContext null
        val user = resolveCurrentUser(canUseRemote = false) ?: return@withContext null
        findActiveCashboxForProfile(resolveServerUserId(user), ctx.profileName)
    }

    //TODO: Sincronizar las monedas existentes y activas en el ERP
    private suspend fun resolveSupportedCurrencies(
        profileName: String,
        baseCurrency: String
    ): List<POSCurrencyOption> {
        val modes = runCatching { paymentMethodLocalRepository.getMethodsForProfile(profileName) }
            .getOrElse { emptyList() }
        val mapped = modes.mapNotNull { mode ->
            mode.currency?.takeIf { it.isNotBlank() }?.let { currency ->
                POSCurrencyOption(
                    code = currency,
                    name = currency
                )
            }
        }
        val normalizedBase = baseCurrency.trim()
        val withBase = if (mapped.any { it.code.equals(normalizedBase, ignoreCase = true) }) {
            mapped
        } else {
            mapped + POSCurrencyOption(
                code = normalizedBase,
                name = normalizedBase
            )
        }
        val distinct = withBase.distinctBy { it.code.uppercase() }
        return distinct.ifEmpty {
            listOf(
                POSCurrencyOption(
                    code = normalizedBase,
                    name = normalizedBase
                )
            )
        }
    }

    private fun buildLocalOpeningEntryId(
        profileName: String,
        userId: String,
        now: Long
    ): String {
        val normalizedProfile = profileName.trim().uppercase().take(12)
        val normalizedUser = userId.substringBefore('@').uppercase().take(8)
        return "LOCAL-OPEN-$normalizedProfile-$normalizedUser-$now"
    }

    private fun buildLocalClosingEntryId(
        profileName: String,
        userId: String,
        now: Long
    ): String {
        val normalizedProfile = profileName.trim().uppercase().take(12)
        val normalizedUser = userId.substringBefore('@').uppercase().take(8)
        return "LOCAL-CLOSE-$normalizedProfile-$normalizedUser-$now"
    }

    private suspend fun resolvePaymentModes(
        profileName: String
    ): List<POSPaymentModeOption> {
        return runCatching { paymentMethodLocalRepository.getMethodsForProfile(profileName) }
            .getOrElse { emptyList() }
            .map { method ->
                POSPaymentModeOption(
                    name = method.mopName,
                    modeOfPayment = method.mopName,
                    account = method.account,
                    currency = method.currency,
                    type = method.type,
                    allowInReturns = method.allowInReturns,
                )
            }
    }

    suspend fun updateClosingAmounts(
        cashboxId: Long,
        closingByMode: Map<String, Double>
    ) = withContext(Dispatchers.IO) {
        closingByMode.forEach { (mode, amount) ->
            cashboxDao.updateClosingAmount(cashboxId, mode, amount)
        }
    }

    suspend fun registerInternalTransfer(
        sourceModeOfPayment: String,
        targetModeOfPayment: String,
        amount: Double,
        note: String?
    ) = withContext(Dispatchers.IO) {
        require(amount > 0.0) { "El monto debe ser mayor que cero." }
        val ctx = currentContext ?: initializeContext()
            ?: error("No hay una caja abierta para registrar transferencia interna.")
        val cashboxId = ctx.cashboxId ?: error("Caja activa no disponible.")
        val sourceMode = sourceModeOfPayment.trim()
        val targetMode = targetModeOfPayment.trim()
        require(sourceMode.isNotBlank()) { "Modo de pago origen requerido." }
        require(targetMode.isNotBlank()) { "Modo de pago destino requerido." }
        require(!sourceMode.equals(targetMode, ignoreCase = true)) {
            "Origen y destino deben ser distintos."
        }

        val updated = cashboxDao.decreaseOpeningAmount(
            cashboxId = cashboxId,
            modeOfPayment = sourceMode,
            amount = amount
        )
        if (updated <= 0) {
            val available = cashboxDao.getOpeningAmountForMode(cashboxId, sourceMode) ?: 0.0
            error("Fondos insuficientes en apertura para $sourceMode. Disponible: $available")
        }
        val targetOpening = cashboxDao.getOpeningAmountForMode(cashboxId, targetMode)
            ?: error("Modo destino no existe en apertura: $targetMode")
        cashboxDao.increaseOpeningAmount(
            cashboxId = cashboxId,
            modeOfPayment = targetMode,
            amount = amount
        )
        val remaining = cashboxDao.getOpeningAmountForMode(cashboxId, sourceMode) ?: 0.0
        val targetNow = targetOpening + amount
        AppLogger.info(
            "internal-transfer: profile=${ctx.profileName} cashbox=$cashboxId from=$sourceMode to=$targetMode amount=$amount fromRemaining=$remaining toNow=$targetNow note=${note?.trim().orEmpty()}"
        )
    }

    suspend fun registerCashMovement(
        modeOfPayment: String,
        amount: Double,
        isIncoming: Boolean,
        note: String?
    ) = withContext(Dispatchers.IO) {
        require(amount > 0.0) { "El monto debe ser mayor que cero." }
        val ctx = currentContext ?: initializeContext()
            ?: error("No hay una caja abierta para registrar movimiento.")
        val cashboxId = ctx.cashboxId ?: error("Caja activa no disponible.")
        val mode = modeOfPayment.trim()
        require(mode.isNotBlank()) { "Modo de pago requerido." }

        if (isIncoming) {
            val opening = cashboxDao.getOpeningAmountForMode(cashboxId, mode)
                ?: error("Modo no existe en apertura: $mode")
            cashboxDao.increaseOpeningAmount(cashboxId, mode, amount)
            AppLogger.info(
                "cash-movement: type=receive profile=${ctx.profileName} cashbox=$cashboxId mode=$mode amount=$amount openingBefore=$opening openingNow=${opening + amount} note=${note?.trim().orEmpty()}"
            )
        } else {
            val updated = cashboxDao.decreaseOpeningAmount(cashboxId, mode, amount)
            if (updated <= 0) {
                val available = cashboxDao.getOpeningAmountForMode(cashboxId, mode) ?: 0.0
                error("Fondos insuficientes en apertura para $mode. Disponible: $available")
            }
            val remaining = cashboxDao.getOpeningAmountForMode(cashboxId, mode) ?: 0.0
            AppLogger.info(
                "cash-movement: type=pay profile=${ctx.profileName} cashbox=$cashboxId mode=$mode amount=$amount openingNow=$remaining note=${note?.trim().orEmpty()}"
            )
        }
    }

    suspend fun resolveExchangeRateBetween(
        fromCurrency: String,
        toCurrency: String,
        allowNetwork: Boolean = false
    ): Double? {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()
        if (from.isBlank() || to.isBlank()) return null
        if (from == to) return 1.0
        return if (allowNetwork) {
            exchangeRateRepository.getRate(from, to)
        } else {
            exchangeRateRepository.getLocalRate(from, to)
        }
    }

    private suspend fun resolveExchangeRate(baseCurrency: String): Double {
        val normalized = baseCurrency.trim().uppercase()
        if (normalized == "USD") return 1.0

        val cached = exchangeRateRepository.getLocalRate("USD", normalized)
        if (cached != null && cached > 0.0) {
            return cached
        }

        return exchangeRatePreferences.loadManualRate()
            ?: ExchangeRatePreferences.DEFAULT_MANUAL_RATE
    }
}
