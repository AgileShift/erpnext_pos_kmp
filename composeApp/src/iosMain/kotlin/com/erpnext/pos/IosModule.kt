package com.erpnext.pos

import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.auth.InstanceSwitcher
import com.erpnext.pos.auth.IosInstanceSwitcher
import com.erpnext.pos.utils.NetworkMonitor
import org.koin.dsl.module

val iosModule = module {
    single<TokenStore> { IosTokenStore() }
    single<AuthInfoStore> { get<TokenStore>() as IosTokenStore }
    single<TransientAuthStore> { get<TokenStore>() as IosTokenStore }
    single { NetworkMonitor() }
    single<InstanceSwitcher> { IosInstanceSwitcher() }

    // DB Builder First
    single { DatabaseBuilder() }
    single<AppDatabase> { get<DatabaseBuilder>().build() }

    // DAO after builder
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().itemReorderDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().posProfileDao() }
    single { get<AppDatabase>().posProfileLocalDao() }
    single { get<AppDatabase>().posProfilePaymentMethodDao() }
    single { get<AppDatabase>().modeOfPaymentDao() }
    single { get<AppDatabase>().paymentTermDao() }
    single { get<AppDatabase>().deliveryChargeDao() }
    single { get<AppDatabase>().exchangeRateDao() }
    single { get<AppDatabase>().cashboxDao() }
    single { get<AppDatabase>().customerDao() }
    single { get<AppDatabase>().customerOutboxDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().saleInvoiceDao() }
    single { get<AppDatabase>().posOpeningDao() }
    single { get<AppDatabase>().posOpeningEntryLinkDao() }
    single { get<AppDatabase>().posClosingDao() }
    single { get<AppDatabase>().companyDao() }
    single { get<AppDatabase>().customerGroupDao() }
    single { get<AppDatabase>().territoryDao() }
    single { get<AppDatabase>().contactDao() }
    single { get<AppDatabase>().addressDao() }
    single { get<AppDatabase>().supplierDao() }
    single { get<AppDatabase>().companyAccountDao() }

    // single<AuthNavigator> { IosAuthNavigator() }
}
