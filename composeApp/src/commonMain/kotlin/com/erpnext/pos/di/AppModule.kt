@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.di

import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.data.repositories.CheckoutRepository
import com.erpnext.pos.data.repositories.ContactRepository
import com.erpnext.pos.data.repositories.AddressRepository
import com.erpnext.pos.data.repositories.CompanyRepository
import com.erpnext.pos.data.repositories.CustomerGroupRepository
import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.data.repositories.CustomerSyncRepository
import com.erpnext.pos.data.repositories.DeliveryChargesRepository
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.data.repositories.ModeOfPaymentRepository
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.data.repositories.ClosingEntrySyncRepository
import com.erpnext.pos.data.repositories.OpeningEntrySyncRepository
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodSyncRepository
import com.erpnext.pos.data.repositories.PaymentTermsRepository
import com.erpnext.pos.data.repositories.POSProfileRepository
import com.erpnext.pos.data.repositories.PaymentEntryRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.TerritoryRepository
import com.erpnext.pos.data.repositories.PosOpeningRepository
import com.erpnext.pos.data.repositories.UserRepository
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.domain.repositories.IPOSRepository
import com.erpnext.pos.domain.repositories.IUserRepository
import com.erpnext.pos.domain.usecases.AdjustLocalInventoryUseCase
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.CreateCustomerUseCase
import com.erpnext.pos.domain.usecases.CreatePaymentEntryUseCase
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceLocalUseCase
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceRemoteOnlyUseCase
import com.erpnext.pos.domain.usecases.FetchBillingProductsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalWithStateUseCase
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceUseCase
import com.erpnext.pos.domain.usecases.FetchBillingProductsWithPriceUseCase
import com.erpnext.pos.domain.usecases.FetchCategoriesUseCase
import com.erpnext.pos.domain.usecases.FetchClosingEntriesUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchDeliveryChargesLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerGroupsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchInventoryItemUseCase
import com.erpnext.pos.domain.usecases.FetchPendingInvoiceUseCase
import com.erpnext.pos.domain.usecases.FetchOutstandingInvoicesForCustomerUseCase
import com.erpnext.pos.domain.usecases.FetchOutstandingInvoicesLocalForCustomerUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceLocalUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceRemoteUseCase
import com.erpnext.pos.domain.usecases.SyncSalesInvoiceFromRemoteUseCase
import com.erpnext.pos.domain.usecases.FetchPaymentTermsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchTerritoriesLocalUseCase
import com.erpnext.pos.domain.usecases.PushPendingCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.domain.usecases.LoadHomeMetricsUseCase
import com.erpnext.pos.domain.usecases.MarkSalesInvoiceSyncedUseCase
import com.erpnext.pos.domain.usecases.LogoutUseCase
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentUseCase
import com.erpnext.pos.domain.usecases.SaveInvoicePaymentsUseCase
import com.erpnext.pos.domain.policy.DatePolicy
import com.erpnext.pos.domain.policy.DefaultPolicy
import com.erpnext.pos.domain.policy.PolicyInput
import com.erpnext.pos.domain.usecases.GetCompanyInfoUseCase
import com.erpnext.pos.domain.usecases.UpdateLocalInvoiceFromRemoteUseCase
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.datasources.CustomerOutboxLocalSource
import com.erpnext.pos.localSource.datasources.ContactLocalSource
import com.erpnext.pos.localSource.datasources.AddressLocalSource
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.datasources.DeliveryChargeLocalSource
import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.localSource.datasources.ModeOfPaymentLocalSource
import com.erpnext.pos.localSource.datasources.PaymentTermLocalSource
import com.erpnext.pos.localSource.datasources.CustomerGroupLocalSource
import com.erpnext.pos.localSource.datasources.TerritoryLocalSource
import com.erpnext.pos.localSource.datasources.POSProfileLocalSource
import com.erpnext.pos.localSource.preferences.ExchangeRatePreferences
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localSource.preferences.OpeningSessionPreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.ThemePreferences
import com.erpnext.pos.localSource.preferences.PreferenceStoreProvider
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.api.defaultEngine
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.oauth.refreshAuthToken
import com.erpnext.pos.remoteSource.datasources.CustomerRemoteSource
import com.erpnext.pos.remoteSource.datasources.InventoryRemoteSource
import com.erpnext.pos.remoteSource.datasources.ModeOfPaymentRemoteSource
import com.erpnext.pos.remoteSource.datasources.POSProfileRemoteSource
import com.erpnext.pos.remoteSource.datasources.SalesInvoiceRemoteSource
import com.erpnext.pos.remoteSource.datasources.UserRemoteSource
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.toBearerToken
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.sync.LegacyPushSyncManager
import com.erpnext.pos.sync.PushSyncRunner
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncOrchestrator
import com.erpnext.pos.sync.OpeningGate
import com.erpnext.pos.sync.PosProfileGate
import com.erpnext.pos.di.v2.appModulev2
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.utils.prefsPath
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.BillingViewModel
import com.erpnext.pos.views.customer.CustomerViewModel
import com.erpnext.pos.domain.usecases.FetchCustomerInvoicesLocalForPeriodUseCase
import com.erpnext.pos.views.deliverynote.DeliveryNoteViewModel
import com.erpnext.pos.views.home.HomeViewModel
import com.erpnext.pos.views.home.HomeRefreshController
import com.erpnext.pos.views.billing.BillingResetController
import com.erpnext.pos.views.home.POSProfileViewModel
import com.erpnext.pos.views.inventory.InventoryViewModel
import com.erpnext.pos.views.invoice.InvoiceViewModel
import com.erpnext.pos.views.login.LoginViewModel
import com.erpnext.pos.views.quotation.QuotationViewModel
import com.erpnext.pos.views.salesorder.SalesOrderViewModel
import com.erpnext.pos.views.settings.SettingsViewModel
import com.erpnext.pos.views.splash.SplashViewModel
import com.erpnext.pos.views.paymententry.PaymentEntryViewModel
import com.erpnext.pos.views.salesflow.SalesFlowContextStore
import com.erpnext.pos.views.payment.PaymentHandler
import com.erpnext.pos.views.reconciliation.ReconciliationViewModel
import com.erpnext.pos.auth.AppLifecycleObserver
import com.erpnext.pos.auth.TokenHeartbeat
import com.erpnext.pos.data.repositories.v2.SourceDocumentRepository
import com.erpnext.pos.domain.usecases.CancelSalesInvoiceUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceWithItemsUseCase
import com.erpnext.pos.domain.usecases.PartialReturnUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoLocalUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoUseCase
import com.erpnext.pos.domain.usecases.v2.LoadSourceDocumentsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

val appModule = module {

    //region Core DI
    single(named("tokenHttpClient")) {
        HttpClient(defaultEngine()) {
            expectSuccess = true
        }
    }

    single {
        val tokenStore: TokenStore = get()
        val authStore: AuthInfoStore? = getOrNull()
        val tokenRefreshClient: HttpClient = get(named("tokenHttpClient"))

        HttpClient(defaultEngine()) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        print("KtorClient -> $message")
                    }
                }
                level = LogLevel.ALL
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
            expectSuccess = true

            authStore?.let { authInfoStore ->
                install(Auth) {
                    bearer {
                        loadTokens {
                            val currentTokens = tokenStore.load() ?: return@loadTokens null
                            val shouldRefresh = shouldRefreshToken(currentTokens.id_token)
                            if (!shouldRefresh) {
                                return@loadTokens currentTokens.toBearerToken()
                            }
                            val refreshToken = currentTokens.refresh_token
                                ?: return@loadTokens if (TokenUtils.isValid(currentTokens.id_token)) {
                                    currentTokens.toBearerToken()
                                } else {
                                    tokenStore.clear()
                                    null
                                }
                            val refreshed = runCatching {
                                refreshAuthToken(tokenRefreshClient, authInfoStore, refreshToken)
                            }.getOrElse { throwable ->
                                AppSentry.capture(throwable, "loadTokens refresh failed")
                                AppLogger.warn("loadTokens refresh failed", throwable)
                                tokenStore.clear()
                                return@loadTokens null
                            }
                            tokenStore.save(refreshed)
                            BearerTokens(
                                refreshed.access_token,
                                refreshed.refresh_token ?: currentTokens.refresh_token
                            )
                        }
                        refreshTokens {
                            val currentTokens = tokenStore.load() ?: return@refreshTokens null
                            val refreshToken =
                                currentTokens.refresh_token ?: return@refreshTokens null
                            val refreshed = runCatching {
                                refreshAuthToken(tokenRefreshClient, authInfoStore, refreshToken)
                            }.getOrElse { throwable ->
                                AppSentry.capture(throwable, "refreshTokens failed")
                                AppLogger.warn("refreshTokens failed", throwable)
                                tokenStore.clear()
                                return@refreshTokens null
                            }
                            tokenStore.save(refreshed)
                            BearerTokens(
                                refreshed.access_token,
                                refreshed.refresh_token ?: currentTokens.refresh_token
                            )
                        }
                        sendWithoutRequest { true }
                    }
                }
            }
        }
    }

    single(named("apiService")) {
        APIService(
            client = get(),
            store = get(),
            authStore = get(),
            tokenClient = get(named("tokenHttpClient"))
        )
    }

    single { SnackbarController() }
    single { SalesFlowContextStore() }
    single { HomeRefreshController() }
    single { BillingResetController() }

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { NavigationManager(get()) }
    single {
        SessionRefresher(
            tokenStore = get(),
            apiService = get(named("apiService")),
            navigationManager = get(),
            networkMonitor = get()
        )
    }
    single { AppLifecycleObserver() }
    single {
        TokenHeartbeat(
            scope = get(),
            sessionRefresher = get(),
            networkMonitor = get(),
            tokenStore = get(),
            lifecycleObserver = get()
        ).apply { start(intervalMinutes = 1) }
    }
    single {
        PaymentHandler(
            api = get(named("apiService")),
            createPaymentEntryUseCase = get(),
            saveInvoicePaymentsUseCase = get(),
            exchangeRateRepository = get(),
            networkMonitor = get()
        )
    }
    single { PreferenceStoreProvider.get(prefsPath()) }
    single { ExchangeRatePreferences(get()) }
    single { LanguagePreferences(get()) }
    single { OpeningSessionPreferences(get()) }
    single { SyncPreferences(get()) }
    single { ThemePreferences(get()) }
    single<DatePolicy> { DefaultPolicy(PolicyInput()) }
    single<CashBoxManager> {
        CashBoxManager(
            api = get(named("apiService")),
            profileDao = get(),
            openingDao = get(),
            openingEntryLinkDao = get(),
            closingDao = get(),
            companyDao = get(),
            cashboxDao = get(),
            userDao = get(),
            exchangeRatePreferences = get(),
            exchangeRateRepository = get(),
            openingEntrySyncRepository = get(),
            paymentMethodLocalRepository = get(),
            salesInvoiceDao = get(),
            sessionRefresher = get(),
            networkMonitor = get()
        )
    }
    single(named("apiServiceV2")) { APIServiceV2(get(), get(), get()) }

    single<PushSyncRunner> {
        LegacyPushSyncManager(
            invoiceRepository = get(),
            invoiceLocalSource = get(),
            modeOfPaymentDao = get(),
            paymentEntryUseCase = get(),
            exchangeRateRepository = get(),
            openingEntrySyncRepository = get(),
            closingEntrySyncRepository = get(),
            customerSyncRepository = get(),
            cashBoxManager = get()
        )
    }

    //region Reconciliation
    single {
        ReconciliationViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    //endregion


    single { SyncContextProvider(get(), get()) }
    single<SyncManager> {
        SyncManager(
            invoiceRepo = get(),
            customerRepo = get(),
            inventoryRepo = get(),
            modeOfPaymentRepo = get(),
            posProfilePaymentMethodSyncRepository = get(),
            paymentTermsRepo = get(),
            deliveryChargesRepo = get(),
            contactRepo = get(),
            addressRepo = get(),
            customerGroupRepo = get(),
            territoryRepo = get(),
            exchangeRateRepo = get(),
            syncPreferences = get(),
            companyInfoRepo = get(),
            cashBoxManager = get(),
            posProfileDao = get(),
            networkMonitor = get(),
            sessionRefresher = get(),
            syncContextProvider = get(),
            pushSyncManager = get()
        )
    }
    //endregion

    //region Login DI
    single { LoginViewModel(get(), get(named("apiService")), get(), get(), get(), get()) }
    //endregion

    //region Splash DI
    single { SplashViewModel(get(), get(), get(), get(named("apiService"))) }
    //endregion

    //region Company
    single { CompanyRepository(get(named("apiService")), get()) }
    //endregion

    //region Inventory
    single { InventoryRemoteSource(get(named("apiService"))) }
    single { InventoryLocalSource(get(), get()) }
    single { InventoryRepository(get(), get(), get()) }
    single { InventoryViewModel(get(), get(), get()) }
    //endregion

    //region Mode of Payment
    single { ModeOfPaymentRemoteSource(get(named("apiService"))) }
    single { ModeOfPaymentLocalSource(get()) }
    single { ModeOfPaymentRepository(get(), get(), get()) }
    single { PosProfilePaymentMethodLocalRepository(get()) }
    single {
        PosProfilePaymentMethodSyncRepository(
            apiService = get(named("apiService")),
            modeOfPaymentRemoteSource = get(),
            posProfileDao = get(),
            posProfileLocalDao = get(),
            posProfilePaymentMethodDao = get(),
            modeOfPaymentDao = get()
        )
    }
    single {
        SyncOrchestrator(
            networkMonitor = get(),
            sessionRefresher = get(),
            posProfilePaymentMethodSyncRepository = get()
        )
    }
    single { OpeningGate(get(), get()) }
    single { PosProfileGate(get(), get(), get()) }
    single {
        OpeningEntrySyncRepository(
            posOpeningRepository = get(),
            openingEntryDao = get(),
            openingEntryLinkDao = get(),
            cashboxDao = get(),
            salesInvoiceDao = get()
        )
    }
    single {
        ClosingEntrySyncRepository(
            api = get(named("apiService")),
            cashboxDao = get(),
            openingEntryLinkDao = get(),
            openingEntryDao = get(),
            closingDao = get(),
            salesInvoiceDao = get()
        )
    }
    //endregion

    //region POS Profile
    single { POSProfileLocalSource(get(), get()) }
    single { POSProfileRemoteSource(get(named("apiService")), get()) }
    single<IPOSRepository> { POSProfileRepository(get(), get()) }
    single { POSProfileViewModel(get(), get(), get()) }
    //endregion

    //region Customer
    single { CustomerRemoteSource(get(named("apiService"))) }
    single { CustomerLocalSource(get(), get()) }
    single { CustomerOutboxLocalSource(get()) }
    single { CustomerRepository(get(), get(), get(), get()) }
    single { CustomerSyncRepository(get(named("apiService")), get(), get(), get(), get()) }
    single { FetchCustomerInvoicesLocalForPeriodUseCase(get()) }
    single { CreateCustomerUseCase(get(), get()) }
    single { PushPendingCustomersUseCase(get()) }
    single { CustomerGroupLocalSource(get()) }
    single { TerritoryLocalSource(get()) }
    single { CustomerGroupRepository(get(named("apiService")), get()) }
    single { TerritoryRepository(get(named("apiService")), get()) }
    single { ContactLocalSource(get()) }
    single { AddressLocalSource(get()) }
    single { ContactRepository(get(named("apiService")), get()) }
    single { AddressRepository(get(named("apiService")), get()) }
    single {
        CustomerViewModel(
            cashboxManager = get(),
            fetchCustomersUseCase = get(),
            checkCustomerCreditUseCase = get(),
            fetchCustomerDetailUseCase = get(),
            fetchOutstandingInvoicesUseCase = get(),
            fetchCustomerInvoicesForPeriodUseCase = get(),
            fetchSalesInvoiceLocalUseCase = get(),
            fetchSalesInvoiceWithItemsUseCase = get(),
            modeOfPaymentDao = get(),
            paymentHandler = get(),
            createCustomerUseCase = get(),
            pushPendingCustomersUseCase = get(),
            fetchCustomerGroupsUseCase = get(),
            fetchTerritoriesUseCase = get(),
            fetchPaymentTermsUseCase = get(),
            companyDao = get(),
            cancelSalesInvoiceUseCase = get(),
            partialReturnUseCase = get(),
            networkMonitor = get()
        )
    }
    //endregion

    //region Home
    single { UserRemoteSource(get(named("apiService")), get()) }
    single { FetchPosProfileInfoUseCase(get()) }
    single { FetchPosProfileInfoLocalUseCase(get()) }
    single {
        HomeViewModel(
            fetchUserInfoUseCase = get(),
            fetchPosProfileUseCase = get(),
            logoutUseCase = get(),
            fetchPosProfileInfoLocalUseCase = get(),
            contextManager = get(),
            posProfileDao = get(),
            paymentMethodLocalRepository = get(),
            syncManager = get(),
            syncPreferences = get(),
            navManager = get(),
            loadHomeMetricsUseCase = get(),
            posProfileGate = get(),
            openingGate = get(),
            homeRefreshController = get()
        )
    }
    single<IUserRepository> { UserRepository(get(), get()) }
    //endregion

    //region Invoices
    single { SalesInvoiceRemoteSource(get(named("apiService")), get()) }
    single { InvoiceViewModel(get(), get(), get()) }
    single { SalesInvoiceRepository(get(), get(), get(), get(), get()) }
    single { CreateSalesInvoiceUseCase(get()) }
    single { CancelSalesInvoiceUseCase(get(), get(), get(), get()) }
    //endregion

    //region Payment Terms
    single { PaymentTermLocalSource(get()) }
    single { DeliveryChargeLocalSource(get()) }
    single { ExchangeRateLocalSource(get()) }
    single { PaymentTermsRepository(get(named("apiService")), get()) }
    single { DeliveryChargesRepository(get(named("apiService")), get()) }
    single { ExchangeRateRepository(get(), get(named("apiService"))) }
    single { PosOpeningRepository(get(named("apiService"))) }
    //endregion

    //region Quotation/Sales Order/Delivery Note
    single { QuotationViewModel(get()) }
    single { SalesOrderViewModel(get()) }
    single { DeliveryNoteViewModel(get()) }
    single { PaymentEntryViewModel(get(), get()) }
    //endregion

    //region Checkout
    //single(named("apiServiceV2")) { APIServiceV2(get(), get(), get()) }
    single { SourceDocumentRepository(get(named("apiServiceV2"))) }
    single { LoadSourceDocumentsUseCase(get()) }

    single { AdjustLocalInventoryUseCase(get()) }
    single { PaymentEntryRepository(get(named("apiService"))) }
    single {
        BillingViewModel(
            customersUseCase = get<FetchCustomersLocalUseCase>(),
            itemsUseCase = get<FetchBillingProductsLocalUseCase>(),
            adjustLocalInventoryUseCase = get(),
            contextProvider = get(),
            modeOfPaymentDao = get(),
            paymentTermsUseCase = get(),
            deliveryChargesUseCase = get(),
            navManager = get(),
            salesFlowStore = get(),
            loadSourceDocumentsUseCase = get(),
            createSalesInvoiceLocalUseCase = get(),
            createSalesInvoiceRemoteOnlyUseCase = get(),
            updateLocalInvoiceFromRemoteUseCase = get(),
            markSalesInvoiceSyncedUseCase = get(),
            api = get(named("apiService")),
            paymentHandler = get(),
            billingResetController = get()
        )
    }
    single { SalesInvoiceRemoteSource(get(named("apiService")), get()) }
    single { InvoiceLocalSource(get()) }
    single { CheckoutRepository(get(), get()) }
    //endregion

    //region Settings
    single { SettingsViewModel(get(), get(), get(), get()) }
    //endregion

    //region UseCases DI
    single { LogoutUseCase(get(named("apiService"))) }
    single { FetchBillingProductsWithPriceUseCase(get()) }
    single { FetchBillingProductsLocalUseCase(get()) }
    single { CheckCustomerCreditUseCase(get()) }
    single { FetchPendingInvoiceUseCase(get()) }
    single { FetchOutstandingInvoicesForCustomerUseCase(get()) }
    single { FetchOutstandingInvoicesLocalForCustomerUseCase(get()) }
    single { FetchSalesInvoiceRemoteUseCase(get()) }
    single { FetchSalesInvoiceLocalUseCase(get()) }
    single { FetchSalesInvoiceWithItemsUseCase(get()) }
    single { SyncSalesInvoiceFromRemoteUseCase(get()) }
    single { CreateSalesInvoiceLocalUseCase(get()) }
    single { CreateSalesInvoiceRemoteOnlyUseCase(get()) }
    single { UpdateLocalInvoiceFromRemoteUseCase(get()) }
    single { SaveInvoicePaymentsUseCase(get()) }
    single { MarkSalesInvoiceSyncedUseCase(get()) }
    single { FetchCustomersUseCase(get()) }
    single { FetchCustomersLocalUseCase(get()) }
    single { FetchCustomersLocalWithStateUseCase(get()) }
    single { FetchPaymentTermsLocalUseCase(get()) }
    single { FetchDeliveryChargesLocalUseCase(get()) }
    single { FetchCustomerGroupsLocalUseCase(get()) }
    single { FetchTerritoriesLocalUseCase(get()) }
    single { FetchCustomerDetailUseCase(get()) }
    single { FetchInventoryItemUseCase(get()) }
    single { FetchCategoriesUseCase(get()) }
    single { FetchClosingEntriesUseCase(get()) }
    single { FetchPosProfileUseCase(get()) }
    single { FetchUserInfoUseCase(get()) }
    single { RegisterInvoicePaymentUseCase(get()) }
    single { CreatePaymentEntryUseCase(get()) }
    single { PartialReturnUseCase(get(), get(), get(), get()) }
    single { LoadHomeMetricsUseCase(get()) }
    single { GetCompanyInfoUseCase(get()) }
    //endregion
}

private const val refreshThresholdSeconds = 10 * 60L

private fun shouldRefreshToken(idToken: String?): Boolean {
    if (!TokenUtils.isValid(idToken)) return true
    val secondsLeft = secondsToExpiry(idToken)
    return secondsLeft != null && secondsLeft <= refreshThresholdSeconds
}

private fun secondsToExpiry(idToken: String?): Long? {
    if (idToken == null) return null
    val claims = TokenUtils.decodePayload(idToken) ?: return null
    val exp = claims["exp"]?.toString()?.toLongOrNull() ?: return null
    val now = Clock.System.now().epochSeconds
    return exp - now
}

fun initKoin(
    config: KoinAppDeclaration? = null, modules: List<Module> = listOf(), builder: DatabaseBuilder
) {
    startKoin {
        config?.invoke(this)
        modules(appModule + appModulev2 + modules)
        koin.get<AppDatabase> { parametersOf(builder) }
    }
}
