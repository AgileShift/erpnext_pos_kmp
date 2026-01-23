package com.erpnext.pos.data

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import java.io.File
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val dbPath = File(System.getProperty("user.home"), ".erpnext-pos/db/erpnext_pos.db")
        print("Database Path -> $dbPath")
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath.absolutePath
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}
