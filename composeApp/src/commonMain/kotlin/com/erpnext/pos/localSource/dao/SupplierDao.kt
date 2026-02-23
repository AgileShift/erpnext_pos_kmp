package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.SupplierEntity

@Dao
interface SupplierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SupplierEntity>)

    @Query("""
        SELECT * FROM tabSupplier
        WHERE is_deleted = 0 AND (disabled IS NULL OR disabled = 0)
        ORDER BY COALESCE(supplier_name, name) ASC
    """)
    suspend fun getAllActive(): List<SupplierEntity>

    @Query("UPDATE tabSupplier SET is_deleted = 1 WHERE is_deleted = 0 AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabSupplier WHERE is_deleted = 1 AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("DELETE FROM tabSupplier WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabSupplier SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()
}
