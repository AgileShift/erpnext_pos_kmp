package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.CompanyAccountEntity

@Dao
interface CompanyAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompanyAccountEntity>)

    @Query("""
        SELECT * FROM tabCompanyAccount
        WHERE is_deleted = 0
          AND (disabled IS NULL OR disabled = 0)
          AND (is_group IS NULL OR is_group = 0)
        ORDER BY name ASC
    """)
    suspend fun getAllActive(): List<CompanyAccountEntity>

    @Query("UPDATE tabCompanyAccount SET is_deleted = 1 WHERE is_deleted = 0 AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabCompanyAccount WHERE is_deleted = 1 AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("DELETE FROM tabCompanyAccount WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabCompanyAccount SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()
}
