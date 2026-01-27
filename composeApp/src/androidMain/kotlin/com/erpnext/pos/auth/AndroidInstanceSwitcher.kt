package com.erpnext.pos.auth

import android.content.Context
import com.erpnext.pos.androidModule
import com.erpnext.pos.data.DatabaseBuilder
import com.erpnext.pos.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin

class AndroidInstanceSwitcher(
    private val context: Context
) : InstanceSwitcher {
    override suspend fun switchInstance(siteUrl: String?) {
        val application = context.applicationContext as? android.app.Application
            ?: return
        stopKoin()
        initKoin(
            { androidContext(application) },
            listOf(androidModule),
            builder = DatabaseBuilder(application)
        )
    }
}
