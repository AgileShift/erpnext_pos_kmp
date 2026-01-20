package com.erpnext.pos.data

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import java.io.File
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val dbPath = File(System.getProperty("user.home"), ".erpnext-pos/db/erpnext_pos.db")
        print("Database Path -> ${dbPath}")
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath.absolutePath
        ).setDriver(BundledSQLiteDriver())
            .addMigrations(AppDatabaseMigrations.MIGRATION_23_24)
            .addMigrations(AppDatabaseMigrations.MIGRATION_24_25)
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(false)
            .build()
    }
}
