package com.erpnext.pos.views

import androidx.compose.runtime.collectAsState
import com.erpnext.pos.data.mappers.buildClosingEntryDto
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.localSource.dao.CashboxDao
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
import com.erpnext.pos.localSource.preferences.ExchangeRatePreferences
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.data.repositories.PosOpeningRepository
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.data.repositories.ClosingEntrySyncRepository
import com.erpnext.pos.data.repositories.OpeningEntrySyncRepository
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.UserEntity
import io.ktor.util.date.getTimeMillis
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.buildPaymentReconciliationSeeds
import com.erpnext.pos.utils.normalizeCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.erpnext.pos.data.repositories.CurrencySettingsRepository
import kotlinx.coroutines.flow.StateFlow

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
    val currency: String,
    val partyAccountCurrency: String,
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
    private val generalPreferences: com.erpnext.pos.localSource.preferences.GeneralPreferences,
    private val currencySettingsRepository: CurrencySettingsRepository,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor
) {

    //Contexto actual del POS cargado en memoria
    private var currentContext: POSContext? = null
    private val _contextFlow: MutableStateFlow<POSContext?> = MutableStateFlow(null)
    val contextFlow = _contextFlow.asStateFlow()

    private val _cashboxState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val cashboxState = _cashboxState.asStateFlow()

    suspend fun initializeContext(): POSContext? = withContext(Dispatchers.IO) {
        currencySettingsRepository.loadCached()
        val user = userDao.getUserInfo() ?: return@withContext null
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        if (isOnline && sessionRefresher.ensureValidSession()) {
            runCatching { openingEntrySyncRepository.pushPending() }
                .onFailure { AppLogger.warn("initializeContext: sync openings failed", it) }
            openingEntrySyncRepository.repairActiveOpenings()
            runCatching { closingEntrySyncRepository.reconcileRemoteClosingsForActiveCashboxes() }
                .onFailure { AppLogger.warn("initializeContext: reconcile remote closings failed", it) }
        } else {
            openingEntrySyncRepository.repairActiveOpenings()
        }
        val company = companyDao.getCompanyInfo()
        val activeProfile = profileDao.getActiveProfile()
        val activeCashbox = activeProfile?.let {
            cashboxDao.getActiveEntry(user.email, it.profileName).firstOrNull()
        }
        val resolvedCashbox = activeCashbox ?: cashboxDao.getActiveEntryForUser(user.email)
        if (resolvedCashbox == null && isOnline && activeProfile != null) {
            val remoteOpen = resolveRemoteOpenSession(user, activeProfile.profileName)
            if (remoteOpen != null) {
                val restored = runCatching {
                    restoreRemoteOpening(activeProfile, remoteOpen.name, user.email)
                }.getOrNull()
                if (restored != null) {
                    val restoredCashbox =
                        cashboxDao.getActiveEntry(user.email, activeProfile.profileName).firstOrNull()
                    if (restoredCashbox != null) {
                        _cashboxState.update { true }
                        return@withContext buildContextFrom(
                            user = user,
                            profile = activeProfile,
                            cashboxId = restoredCashbox.cashbox.localId,
                            company = company
                        )
                    }
                }
            }
        }
        val profile = when {
            activeProfile != null && activeCashbox != null -> activeProfile
            resolvedCashbox != null -> profileDao.getPOSProfile(resolvedCashbox.cashbox.posProfile)
            else -> return@withContext null
        }
        if (resolvedCashbox != null) {
            profileDao.updateProfileState(user.username, profile.profileName, true)
        }
        val exchangeRate = resolveExchangeRate(profile.currency)
        val allowedCurrencies = resolveSupportedCurrencies(profile.profileName, profile.currency)
        val paymentModes = resolvePaymentModes(profile.profileName)

        _cashboxState.update { resolvedCashbox != null }

        currentContext = POSContext(
            cashier = user.toBO(),
            username = user.username ?: "",
            profileName = profile.profileName,
            company = company?.companyName ?: profile.company,
            companyCurrency = company?.defaultCurrency ?: profile.currency,
            allowNegativeStock = generalPreferences.allowNegativeStock.firstOrNull() == true,
            partyAccountCurrency = company?.defaultCurrency ?: profile.currency,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = resolvedCashbox != null,
            cashboxId = resolvedCashbox?.cashbox?.localId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes
        )
        _contextFlow.value = currentContext
        currentContext
    }

    suspend fun openCashBox(
        entry: POSProfileSimpleBO,
        amounts: List<PaymentModeWithAmount>
    ): POSContext? = withContext(Dispatchers.IO) {
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        if (isOnline && !sessionRefresher.ensureValidSession()) {
            AppLogger.warn("openCashBox: invalid session, aborting")
            return@withContext null
        }
        val user = userDao.getUserInfo() ?: return@withContext null
        val existing = cashboxDao.getActiveEntry(user.email, entry.name).firstOrNull()
        if (existing != null) {
            if (isOnline) {
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
            val allowedCurrencies = resolveSupportedCurrencies(profile.profileName, profile.currency)
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
                currency = profile.currency,
                exchangeRate = exchangeRate,
                allowedCurrencies = allowedCurrencies,
                paymentModes = paymentModes,
                cashier = user.toBO(),
                partyAccountCurrency = company?.defaultCurrency ?: profile.currency,
            )
            _contextFlow.value = currentContext
            return@withContext currentContext
        }
        if (isOnline) {
            val remoteOpen = resolveRemoteOpenSession(user, entry.name)
            if (remoteOpen != null) {
                val restored = runCatching {
                    restoreRemoteOpening(
                        profileDao.getPOSProfile(entry.name),
                        remoteOpen.name,
                        user.email,
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
                    return@withContext currentContext
                }
            }
        }
        val now = getTimeMillis()
        val periodStart = now.toErpDateTime()
        val openingEntryName = buildLocalOpeningEntryId(entry.name, user.email, now)
        val openingEntry = POSOpeningEntryEntity(
            name = openingEntryName,
            posProfile = entry.name,
            company = entry.company,
            periodStartDate = periodStart,
            postingDate = periodStart,
            user = user.email,
            pendingSync = true
        )
        openingDao.insert(openingEntry)

        val cashbox = CashboxEntity(
            localId = 0,
            posProfile = entry.name,
            company = entry.company,
            periodStartDate = periodStart,
            user = user.email,
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
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes,
            cashier = user.toBO(),
            partyAccountCurrency = profile.currency,
        )
        _contextFlow.value = currentContext
        currentContext!!
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
        userEmail: String,
        fallbackAmounts: List<PaymentModeWithAmount> = emptyList()
    ): Long {
        val detail = runCatching { api.getPOSOpeningEntry(remoteName) }.getOrNull()
        val periodStart = detail?.periodStartDate ?: getTimeMillis().toErpDateTime()
        val postingDate = detail?.postingDate ?: periodStart
        val openingEntry = POSOpeningEntryEntity(
            name = remoteName,
            posProfile = profile.profileName,
            company = detail?.company ?: profile.company,
            periodStartDate = periodStart,
            postingDate = postingDate,
            user = detail?.user ?: userEmail,
            pendingSync = false
        )
        openingDao.insert(openingEntry)
        val cashbox = CashboxEntity(
            localId = 0,
            posProfile = profile.profileName,
            company = detail?.company ?: profile.company,
            periodStartDate = periodStart,
            user = detail?.user ?: userEmail,
            status = true,
            pendingSync = false,
            openingEntryId = remoteName,
        )
        val balanceDetails = if (!detail?.balanceDetails.isNullOrEmpty()) {
            detail!!.balanceDetails.map {
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
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes,
            partyAccountCurrency = company?.defaultCurrency ?: profile.currency
        )
    }

    private suspend fun resolveRemoteOpenSession(
        user: UserEntity,
        profileName: String
    ): com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto? {
        val candidates = listOfNotNull(
            user.email?.takeIf { it.isNotBlank() },
            user.username?.takeIf { it.isNotBlank() },
            user.name?.takeIf { it.isNotBlank() }
        ).distinct()
        candidates.forEach { candidate ->
            val remote = runCatching {
                posOpeningRepository.getOpenSession(candidate, profileName)
            }.onFailure {
                AppLogger.warn("resolveRemoteOpenSession failed for user=$candidate profile=$profileName", it)
            }.getOrNull()
            if (remote != null) return remote
        }
        val byProfile = runCatching {
            posOpeningRepository.getOpenSessionsForProfile(profileName)
        }.onFailure {
            AppLogger.warn("resolveRemoteOpenSession profile lookup failed for profile=$profileName", it)
        }.getOrNull().orEmpty()
        val normalized = candidates.map { it.trim().lowercase() }.toSet()
        return byProfile.firstOrNull { summary ->
            val remoteUser = summary.user?.trim()?.lowercase()
            remoteUser != null && normalized.contains(remoteUser)
        }
    }

    //TODO Armar y enviar el POSClosingEntry
    suspend fun closeCashBox(): POSContext? = withContext(Dispatchers.IO) {
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        if (isOnline && !sessionRefresher.ensureValidSession()) {
            AppLogger.warn("closeCashBox: invalid session, aborting")
            return@withContext null
        }
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) return@withContext null
        val user = userDao.getUserInfo() ?: return@withContext null

        val entry = cashboxDao.getActiveEntry(user.email, ctx.profileName).firstOrNull()
            ?: return@withContext null
        val endMillis = getTimeMillis()
        val startMillis = parseErpDateTimeToEpochMillis(entry.cashbox.periodStartDate)
            ?: endMillis
        var remoteOpeningEntryId = openingEntryLinkDao.getRemoteOpeningEntryName(entry.cashbox.localId)
        if (remoteOpeningEntryId.isNullOrBlank()) {
            runCatching { openingEntrySyncRepository.pushPending() }
                .onFailure { AppLogger.warn("closeCashBox: sync opening failed", it) }
            remoteOpeningEntryId =
                openingEntryLinkDao.getRemoteOpeningEntryName(entry.cashbox.localId)
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
        val resolvedInvoices = if (isOnline) {
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
        val endDate = getTimeMillis().toErpDateTime()
        val dto = buildClosingEntryDto(
            cashbox = entry.cashbox,
            openingEntryId = openingEntryId,
            postingDate = endDate,
            periodEndDate = endDate,
            paymentReconciliation = paymentReconciliation,
            invoices = resolvedInvoices
        )
        if (!remoteOpeningEntryId.isNullOrBlank()) {
            val existing = openingDao.getByName(remoteOpeningEntryId)
            if (existing == null) {
                val local = openingDao.getByName(localOpeningEntryId ?: "") ?: return@withContext null
                openingDao.insert(local.copy(name = remoteOpeningEntryId, pendingSync = false))
            }
        }
        val pce = if (!remoteOpeningEntryId.isNullOrBlank()) {
            runCatching { api.closeCashbox(dto) }
                .onFailure { AppLogger.warn("closeCashBox: remote close failed", it) }
                .getOrNull()
        } else {
            null
        }
        val submitOk = pce?.let { it ->
            runCatching { api.submitPOSClosingEntry(it.name) }
                .onFailure { AppLogger.warn("closeCashBox: submit closing failed", it) }
                .getOrNull()
        }
        val closingEntryId = pce?.name
            ?: buildLocalClosingEntryId(ctx.profileName, user.email, endMillis)
        val pendingSync = pce == null || submitOk == null

        cashboxDao.updateStatus(
            entry.cashbox.localId,
            status = false,
            closingEntryId,
            dto.periodEndDate,
            pendingSync = pendingSync
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
            val updated = runCatching {
                api.setValue(
                    com.erpnext.pos.remoteSource.sdk.ERPDocType.SalesInvoice.path,
                    name,
                    "pos_opening_entry",
                    openingEntryId
                )
                api.setValue(
                    com.erpnext.pos.remoteSource.sdk.ERPDocType.SalesInvoice.path,
                    name,
                    "pos_profile",
                    posProfile
                )
                true
            }.getOrElse {
                AppLogger.warn("closeCashBox: set_value failed for $name", it)
                false
            }
            if (updated) {
                salesInvoiceDao.updateInvoiceOpeningAndProfile(
                    invoiceName = name,
                    posOpeningEntry = openingEntryId,
                    profileId = posProfile
                )
                salesInvoiceDao.updatePaymentsOpeningForInvoice(name, openingEntryId)
                resolved += invoice
            } else {
                AppLogger.warn(
                    "closeCashBox: remote invoice $name without opening entry $openingEntryId"
                )
            }
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
        val user = userDao.getUserInfo()
        if (user == null) {
            emit(null)
            return@flow
        }
        emitAll(
            cashboxDao.getActiveEntry(user.email, ctx.profileName)
                .map { it?.cashbox?.periodStartDate }
        )
    }

    fun activeOpeningEntryId(): Flow<String?> = flow {
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) {
            emit(null)
            return@flow
        }
        val user = userDao.getUserInfo()
        if (user == null) {
            emit(null)
            return@flow
        }
        emitAll(
            cashboxDao.getActiveEntry(user.email, ctx.profileName)
                .map { it?.cashbox?.openingEntryId }
        )
    }

    suspend fun getActiveCashboxWithDetails(): CashboxWithDetails? = withContext(Dispatchers.IO) {
        val ctx = currentContext ?: initializeContext() ?: return@withContext null
        val user = userDao.getUserInfo() ?: return@withContext null
        cashboxDao.getActiveEntry(user.email, ctx.profileName).firstOrNull()
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

    private fun buildLocalOpeningEntryId(profileName: String, userEmail: String, now: Long): String {
        val normalizedProfile = profileName.trim().uppercase().take(12)
        val normalizedUser = userEmail.substringBefore('@').uppercase().take(8)
        return "LOCAL-OPEN-$normalizedProfile-$normalizedUser-$now"
    }

    private fun buildLocalClosingEntryId(profileName: String, userEmail: String, now: Long): String {
        val normalizedProfile = profileName.trim().uppercase().take(12)
        val normalizedUser = userEmail.substringBefore('@').uppercase().take(8)
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

    suspend fun resolveExchangeRateBetween(
        fromCurrency: String,
        toCurrency: String,
        allowNetwork: Boolean = true
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

        val cached = exchangeRateRepository.getRate("USD", normalized)
        if (cached != null && cached > 0.0) {
            return cached
        }

        return exchangeRatePreferences.loadManualRate()
            ?: ExchangeRatePreferences.DEFAULT_MANUAL_RATE
    }
}
