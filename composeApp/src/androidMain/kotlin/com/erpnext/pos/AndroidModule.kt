package com.erpnext.pos

import android.content.Context
import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.navigation.AndroidAuthNavigator
import com.erpnext.pos.navigation.AuthNavigator
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.auth.AndroidInstanceSwitcher
import com.erpnext.pos.auth.InstanceSwitcher
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.TimeProvider
import org.koin.dsl.module

val androidModule = module {
    single<TokenStore> { AndroidTokenStore(get()) }
    single<AuthInfoStore> { get<TokenStore>() as AndroidTokenStore }
    single<TransientAuthStore> { get<TokenStore>() as AndroidTokenStore }
    single<InstanceSwitcher> { AndroidInstanceSwitcher(get()) }
    single<AuthNavigator> { AndroidAuthNavigator() }
    single { NetworkMonitor(get<Context>()) }
    single { TimeProvider() }
    single { (builder: DatabaseBuilder) -> builder.build() }
    single { get<AppDatabase>().itemDao() }
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

    // V2 DAOs
    single { get<AppDatabase>().catalogDaoV2() }
    single { get<AppDatabase>().inventoryDaoV2() }
    single { get<AppDatabase>().customerDaoV2() }
    single { get<AppDatabase>().salesInvoiceDaoV2() }
    single { get<AppDatabase>().syncStatusDaoV2() }
    single { get<AppDatabase>().quotationDaoV2() }
    single { get<AppDatabase>().salesOrderDaoV2() }
    single { get<AppDatabase>().deliveryNoteDaoV2() }
    single { get<AppDatabase>().paymentEntryDaoV2() }
    single { get<AppDatabase>().paymentScheduleDaoV2() }
    single { get<AppDatabase>().posContextDaoV2() }
}
