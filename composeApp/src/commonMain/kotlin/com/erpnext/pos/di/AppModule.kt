package com.erpnext.pos.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.data.repositories.CheckoutRepository
import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.data.repositories.POSProfileRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.UserRepository
import com.erpnext.pos.di.v2.appModulev2
import com.erpnext.pos.domain.repositories.IPOSRepository
import com.erpnext.pos.domain.repositories.IUserRepository
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.FetchBillingProductsWithPriceUseCase
import com.erpnext.pos.domain.usecases.FetchCategoriesUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchInventoryItemUseCase
import com.erpnext.pos.domain.usecases.FetchPendingInvoiceUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.domain.usecases.LogoutUseCase
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.datasources.POSProfileLocalSource
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.api.defaultEngine
import com.erpnext.pos.remoteSource.datasources.CustomerRemoteSource
import com.erpnext.pos.remoteSource.datasources.InventoryRemoteSource
import com.erpnext.pos.remoteSource.datasources.POSProfileRemoteSource
import com.erpnext.pos.remoteSource.datasources.SalesInvoiceRemoteSource
import com.erpnext.pos.remoteSource.datasources.UserRemoteSource
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.BillingViewModel
import com.erpnext.pos.views.customer.CustomerViewModel
import com.erpnext.pos.views.home.HomeViewModel
import com.erpnext.pos.views.home.POSProfileViewModel
import com.erpnext.pos.views.inventory.InventoryViewModel
import com.erpnext.pos.views.invoice.InvoiceViewModel
import com.erpnext.pos.views.login.LoginViewModel
import com.erpnext.pos.views.settings.SettingsViewModel
import com.erpnext.pos.views.splash.SplashViewModel
import io.ktor.client.HttpClient
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
import okio.Path.Companion.toPath
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {

    //region Core DI
    single {
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
            expectSuccess = true
        }
    }

    single(named("apiService")) {
        APIService(
            client = get(), authStore = get(), store = get()
        )
    }

    single { SnackbarController() }

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { NavigationManager(get()) }
    single {
        PreferenceDataStoreFactory.createWithPath {
            "./prefs.preferences_pb".toPath()
        }
    }
    single<CashBoxManager> { CashBoxManager(get(named("apiService")), get(), get(), get(), get(), get()) }
    single<SyncManager> { SyncManager(get(), get(), get(), get()) }
    //endregion

    //region Login DI
    single { LoginViewModel(get(), get(named("apiService")), get(), get(), get()) }
    //endregion

    //region Splash DI
    single { SplashViewModel(get(), get(), get(), get(named("apiService"))) }
    //endregion

    //region Inventory
    single { InventoryRemoteSource(get(named("apiService"))) }
    single { InventoryLocalSource(get(), get()) }
    single { InventoryRepository(get(), get(), get()) }
    single { InventoryViewModel(get(), get(), get()) }
    //endregion

    //region POS Profile
    single { POSProfileLocalSource(get(), get()) }
    single { POSProfileRemoteSource(get(named("apiService")), get(), get()) }
    single<IPOSRepository> { POSProfileRepository(get(), get()) }
    single { POSProfileViewModel(get(), get(), get()) }
    //endregion

    //region Customer
    single { CustomerRemoteSource(get(named("apiService"))) }
    single { CustomerLocalSource(get(), get()) }
    single { CustomerRepository(get(), get(), get()) }
    single { CustomerViewModel(get(), get(), get(), get()) }
    //endregion

    //region Home
    single { UserRemoteSource(get(named("apiService")), get()) }
    single { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    single<IUserRepository> { UserRepository(get()) }
    //endregion

    //region Invoices
    single { SalesInvoiceRemoteSource(get(named("apiService")), get()) }
    single { InvoiceViewModel(get(), get()) }
    single { SalesInvoiceRepository(get(), get(), get()) }
    //endregion

    //region Checkout
    single { BillingViewModel(get(), get(), get()) }
    single { SalesInvoiceRemoteSource(get(named("apiService")), get()) }
    single { InvoiceLocalSource(get()) }
    single { CheckoutRepository(get(), get()) }
    //endregion

    //region Settings
    single { SettingsViewModel() }
    //endregion

    //region UseCases DI
    single { LogoutUseCase(get(named("apiService"))) }
    single { FetchBillingProductsWithPriceUseCase(get()) }
    single { CheckCustomerCreditUseCase(get()) }
    single { FetchPendingInvoiceUseCase(get()) }
    single { FetchCustomersUseCase(get()) }
    single { FetchCustomerDetailUseCase(get()) }
    single { FetchInventoryItemUseCase(get()) }
    single { FetchCategoriesUseCase(get()) }
    single { FetchPosProfileUseCase(get()) }
    single { FetchPosProfileInfoUseCase(get()) }
    single { FetchUserInfoUseCase(get()) }
    //endregion
}

fun initKoin(
    config: KoinAppDeclaration? = null, modules: List<Module> = listOf(), builder: DatabaseBuilder
) {
    startKoin {
        config?.invoke(this)
        modules(appModule + modules)
        koin.get<AppDatabase> { parametersOf(builder) }
    }
}