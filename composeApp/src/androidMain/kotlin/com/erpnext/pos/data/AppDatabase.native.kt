package com.erpnext.pos.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseBuilder(private val context: Context) {
    actual fun build(): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database",
        ).setDriver(BundledSQLiteDriver())
            .addMigrations(AppDatabaseMigrations.MIGRATION_23_24)
            .addMigrations(AppDatabaseMigrations.MIGRATION_24_25)
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(false)
            .build()
}
