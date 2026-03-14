package com.erpnext.pos.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.erpnext.pos.AndroidTokenStore
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.instanceKeyFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

actual class DatabaseBuilder(private val context: Context) {
  private fun resolveDatabaseName(): String {
    val site =
        runCatching { runBlocking { AndroidTokenStore(context).getCurrentSite() } }
            .onFailure { error ->
              AppLogger.warn("DatabaseBuilder: falling back to default database key", error)
            }
            .getOrNull()

    return "app_database_${instanceKeyFromUrl(site)}"
  }

  actual fun build(): AppDatabase =
      Room.databaseBuilder(
              context,
              AppDatabase::class.java,
              resolveDatabaseName(),
          )
          .setDriver(BundledSQLiteDriver())
          .setQueryCoroutineContext(Dispatchers.IO)
          .fallbackToDestructiveMigration(true)
          .build()
}
