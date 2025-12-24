package com.erpnext.pos.data

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import java.io.File
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val dbPath = File(System.getProperty("java.io.tmpdir"), "erpnext_pos.db")
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath.absolutePath
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(false)
            .build()
    }
}