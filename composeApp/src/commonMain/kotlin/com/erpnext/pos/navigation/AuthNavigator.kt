package com.erpnext.pos.navigation

interface AuthNavigator {
    fun openAuthPage(authUrl: String)
}

expect fun provideAutNavigator(): AuthNavigator