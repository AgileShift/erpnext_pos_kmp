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
    fun insertAll(entities: List<CategoryEntity>)

    @Query("SELECT * FROM tabCategory")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM tabcategory")
    fun count(): Int

    @Query("DELETE FROM tabCategory")
    fun deleteAll()
}