package com.erpnext.pos.navigation

import android.content.Intent
import android.util.Log
import com.erpnext.pos.AppContext
import androidx.core.net.toUri

actual fun provideAutNavigator(): AuthNavigator = AndroidAuthNavigator()

class AndroidAuthNavigator() : AuthNavigator {
    private val context = AppContext.get()

    override fun openAuthPage(authUrl: String) {
        Log.d("OAuthIntentDebug", "authUrl: $authUrl")
        val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
        Log.d("OAuthIntentDebug", "Action: ${intent.action}")
        Log.d("OAuthIntentDebug", "Data URI: ${intent.dataString}") // ¡Este es el crucial!
        Log.d("OAuthIntentDebug", "Flags: ${intent.flags}")
        Log.d("OAuthIntentDebug", "Categories: ${intent.categories}")
        Log.d(
            "OAuthIntentDebug",
            "Component: ${intent.component}"
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}