package com.erpnext.pos

import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.navigation.AndroidAuthNavigator
import com.erpnext.pos.navigation.AuthNavigator
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import org.koin.dsl.module

val androidModule = module {
    single<TokenStore> { AndroidTokenStore(get()) }
    single<AuthInfoStore> { get<TokenStore>() as AndroidTokenStore }
    single<TransientAuthStore> { get<TokenStore>() as AndroidTokenStore }
    single<AuthNavigator> { AndroidAuthNavigator() }
    single { (builder: DatabaseBuilder) -> builder.build() }
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().posProfileDao() }
    single { get<AppDatabase>().paymentModesDao() }
    single { get<AppDatabase>().cashboxDao() }
    single { get<AppDatabase>().customerDao() }
    single { get<AppDatabase>().pendingInvoiceDao() }
    single { get<AppDatabase>().categoryDao() }
}