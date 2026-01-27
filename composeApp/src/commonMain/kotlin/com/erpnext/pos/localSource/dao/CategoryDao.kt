package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.erpnext.pos.localSource.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Query("SELECT * FROM tabCategory WHERE is_deleted = 0")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM tabcategory WHERE is_deleted = 0")
    suspend fun count(): Int

    @Query("UPDATE tabCategory SET is_deleted = 1 WHERE is_deleted = 0 AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabCategory WHERE is_deleted = 1 AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("UPDATE tabCategory SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM tabCategory WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()
}
