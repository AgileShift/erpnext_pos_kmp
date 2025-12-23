package com.erpnext.pos.data

import androidx.room.Room
import platform.Foundation.NSHomeDirectory

actual class DatabaseBuilder {
    actual fun build(): AppDatabase {
        val dbPath = NSHomeDirectory() + "/app.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbPath,
        ).build()
    }
}