package com.erpnext.pos.navigation

actual fun provideAutNavigator(): AuthNavigator = IosAuthNavigator()

class IosAuthNavigator() : AuthNavigator {
    override fun openAuthPage(authUrl: String) {
        TODO("Not yet implemented")
    }
}