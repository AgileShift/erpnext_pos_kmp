package com.erpnext.pos.data

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.erpnext.pos.utils.instanceKeyFromUrl

@OptIn(ExperimentalForeignApi::class)
actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val currentSite = NSUserDefaults.standardUserDefaults.stringForKey("current_site")
        val dbPath = documentDirectory() + "/app_${instanceKeyFromUrl(currentSite)}.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath,
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
}
