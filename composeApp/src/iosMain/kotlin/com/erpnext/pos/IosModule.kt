package com.erpnext.pos

import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.utils.NetworkMonitor
import org.koin.dsl.module

val iosModule = module {
    single<TokenStore> { IosTokenStore() }
    single<TransientAuthStore> { get<TokenStore>() as IosTokenStore }
    single { NetworkMonitor() }

    // single<AuthNavigator> { IosAuthNavigator() }
}