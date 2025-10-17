package com.erpnext.pos.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.data.repositories.POSProfileRepository
import com.erpnext.pos.data.repositories.UserRepository
import com.erpnext.pos.domain.repositories.ICustomerRepository
import com.erpnext.pos.domain.repositories.IInventoryRepository
import com.erpnext.pos.domain.repositories.IPOSRepository
import com.erpnext.pos.domain.repositories.IUserRepository
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.CustomerSearchUseCase
import com.erpnext.pos.domain.usecases.FetchCategoriesUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchInventoryItemUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.api.defaultEngine
import com.erpnext.pos.remoteSource.datasources.CustomerRemoteSource
import com.erpnext.pos.remoteSource.datasources.InventoryRemoteSource
import com.erpnext.pos.remoteSource.datasources.POSProfileRemoteSource
import com.erpnext.pos.remoteSource.datasources.UserRemoteSource
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.customer.CustomerViewModel
import com.erpnext.pos.views.home.HomeViewModel
import com.erpnext.pos.views.home.POSProfileViewModel
import com.erpnext.pos.views.inventory.InventoryViewModel
import com.erpnext.pos.views.login.LoginViewModel
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

    single {
        APIService(
            client = get(), authStore = get(), store = get()
        )
    }

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { NavigationManager(get()) }
    single {
        PreferenceDataStoreFactory.createWithPath {
            "./prefs.preferences_pb".toPath()
        }
    }
    single<CashBoxManager> { CashBoxManager(get(), get()) }
    //endregion

    //region Login DI
    single { LoginViewModel(get(), get(), get(), get()) }
    //endregion

    //region Splash DI
    single { SplashViewModel(get(), get()) }
    //endregion

    //region Inventory DI
    single { InventoryRemoteSource(get(), get()) }
    single<IInventoryRepository> { InventoryRepository(get()) }
    single { InventoryViewModel(get(), get(), get(), get(), get()) }
    //endregion

    //region POS Profile
    single { POSProfileRemoteSource(get(), get(), get()) }
    single<IPOSRepository> { POSProfileRepository(get()) }
    single { POSProfileViewModel(get(), get(), get()) }
    //endregion

    //region Customer
    single { CustomerRemoteSource(get(), get()) }
    single { CustomerLocalSource(get()) }
    single<ICustomerRepository> { CustomerRepository(get(), get()) }
    single { CustomerViewModel(get(), get(), get(), get(), get()) }
    //endregion

    //region Home
    single { UserRemoteSource(get(), get()) }
    single { HomeViewModel(get(), get(), get(), get(), get()) }
    single<IUserRepository> { UserRepository(get()) }
    //endregion

    //region UseCases DI
    single { CheckCustomerCreditUseCase(get()) }
    single { CustomerSearchUseCase(get()) }
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