package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.erpnext.pos.localSource.entities.CompanyEntity

@Dao
interface CompanyDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(company: CompanyEntity)

    @Query(
        """
            SELECT * FROM companies
            WHERE is_deleted = 0
        """
    )
    suspend fun getCompanyInfo(): CompanyEntity?

    @Query("SELECT * FROM companies WHERE is_deleted = 0")
    suspend fun getAll(): List<CompanyEntity>

    @Query("UPDATE companies SET is_deleted = 1 WHERE is_deleted = 0 AND company NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM companies WHERE is_deleted = 1 AND company NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("UPDATE companies SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM companies WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()
}
