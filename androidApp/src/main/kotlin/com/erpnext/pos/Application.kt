package com.erpnext.pos

import android.app.Application
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.di.initKoin
import com.erpnext.pos.utils.AppSentry
import com.google.firebase.Firebase
import com.google.firebase.initialize
import org.koin.android.ext.koin.androidContext

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
    }
}
