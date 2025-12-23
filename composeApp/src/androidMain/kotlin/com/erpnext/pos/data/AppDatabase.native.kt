package com.erpnext.pos.data

import android.content.Context
import androidx.room.Room

actual class DatabaseBuilder(private val context: Context) {
    actual fun build(): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database",
        ).fallbackToDestructiveMigration(false)
            .build()
}