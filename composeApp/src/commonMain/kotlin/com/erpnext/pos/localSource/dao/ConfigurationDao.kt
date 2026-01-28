package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ConfigurationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigurationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ConfigurationEntity)

    @Query("SELECT value FROM tabConfiguration WHERE `key` = :key LIMIT 1")
    fun observeValue(key: String): Flow<String?>

    @Query("SELECT * FROM tabConfiguration WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): ConfigurationEntity?

    @Query("DELETE FROM tabConfiguration WHERE `key` = :key")
    suspend fun delete(key: String)
}
