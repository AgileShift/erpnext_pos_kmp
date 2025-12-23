package com.erpnext.pos.localSource.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItems(items: List<ItemEntity>)

    @Query("SELECT * FROM tabItem ORDER BY name ASC")
    fun getAllItemsPaged(): PagingSource<Int, ItemEntity>

    @Query("SELECT * FROM tabItem ORDER BY name ASC")
    fun getAllItems(): List<ItemEntity>

    @Query("SELECT * FROM TABITEM WHERE name == :id")
    fun getItemById(id: String): PagingSource<Int, ItemEntity>

    @Query("SELECT * FROM tabitem WHERE name LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%' OR brand LIKE '%' || :search || '%' OR itemGroup LIKE '%' || :search || '%' ORDER BY name ASC")
    fun getAllFiltered(search: String): PagingSource<Int, ItemEntity>

    @Query("SELECT * FROM tabitem WHERE name LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%' OR brand LIKE '%' || :search || '%' OR itemGroup LIKE '%' || :search || '%' ORDER BY name ASC")
    fun getAllItemsFiltered(search: String): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM tabItem")
    fun countAll(): Int

    @Query("DELETE FROM tabItem")
    fun deleteAll()

    @Query("SELECT * FROM tabItem ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldestItem(): ItemEntity?
}