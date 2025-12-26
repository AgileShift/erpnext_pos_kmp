package com.erpnext.pos.navigation

import android.content.Intent
import android.util.Log
import com.erpnext.pos.AppContext
import androidx.core.net.toUri

actual fun provideAutNavigator(): AuthNavigator = AndroidAuthNavigator()

class AndroidAuthNavigator() : AuthNavigator {
    private val context = AppContext.get()

    override fun openAuthPage(authUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}