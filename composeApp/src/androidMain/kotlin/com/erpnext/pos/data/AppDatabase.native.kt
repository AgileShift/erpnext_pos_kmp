package com.erpnext.pos.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.erpnext.pos.AndroidTokenStore
import com.erpnext.pos.utils.instanceKeyFromUrl
import kotlinx.coroutines.runBlocking

actual class DatabaseBuilder(private val context: Context) {
    actual fun build(): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database_${instanceKeyFromUrl(runBlocking { AndroidTokenStore(context).getCurrentSite() })}",
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
}
