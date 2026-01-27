package com.erpnext.pos.data

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import java.io.File
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.erpnext.pos.DesktopTokenStore
import com.erpnext.pos.utils.instanceKeyFromUrl
import kotlinx.coroutines.runBlocking

actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val instanceKey = instanceKeyFromUrl(runBlocking { DesktopTokenStore().getCurrentSite() })
        val dbPath = File(System.getProperty("user.home"), ".erpnext-pos/db/erpnext_pos_$instanceKey.db")
        print("Database Path -> $dbPath")
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath.absolutePath
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}
