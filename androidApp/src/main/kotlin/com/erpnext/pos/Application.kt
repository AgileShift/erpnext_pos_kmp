package com.erpnext.pos

import android.app.Application
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.di.initKoin
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.notifications.configureInventoryAlertWorker
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)

        Firebase.initialize(this)
        AppSentry.init()

        initKoin(
            {
                androidContext(this@Application)
            },
            listOf(androidModule),
            builder = DatabaseBuilder(this@Application)
        )

        runBlocking {
            val generalPreferences = GlobalContext.get().get<GeneralPreferences>()
            val enabled = generalPreferences.getInventoryAlertsEnabled()
            val hour = generalPreferences.getInventoryAlertHour()
            val minute = generalPreferences.getInventoryAlertMinute()
            configureInventoryAlertWorker(enabled, hour, minute)
        }
    }
}
