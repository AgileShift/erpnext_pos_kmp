package com.erpnext.pos.data

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import java.io.File
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.erpnext.pos.DesktopTokenStore
import com.erpnext.pos.utils.instanceKeyFromUrl
import com.erpnext.pos.utils.AppLogger
import kotlinx.coroutines.runBlocking

actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val currentSite = runBlocking { DesktopTokenStore().getCurrentSite() }
        val instanceKey = instanceKeyFromUrl(currentSite)
        val dbDir = File(System.getProperty("user.home"), ".erpnext-pos/db")
        dbDir.mkdirs()
        val dbPath = File(dbDir, "erpnext_pos_$instanceKey.db")
        AppLogger.info(
            "DatabaseBuilder.desktop site=${currentSite ?: "none"} " +
                "instanceKey=$instanceKey db=${dbPath.absolutePath}"
        )
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath.absolutePath
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}
