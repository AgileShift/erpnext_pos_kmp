package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity

@Dao
interface ModeOfPaymentDao {
    @Query(
        """
        SELECT * 
             FROM tabmodeofpayment
             WHERE company = :company
             AND is_deleted = 0
             ORDER BY mode_of_payment ASC"""
    )
    suspend fun getAll(company: String?): List<ModeOfPaymentEntity>

    @Query("SELECT * FROM tabModeOfPayment WHERE company = :company AND is_deleted = 0")
    suspend fun getAllModes(company: String): List<ModeOfPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllModes(items: List<ModeOfPaymentEntity>)

    @Query("SELECT * FROM tabModeOfPayment WHERE name IN (:names) AND is_deleted = 0")
    suspend fun getByNames(names: List<String>): List<ModeOfPaymentEntity>

    @Query("SELECT MAX(last_synced_at) FROM tabModeOfPayment WHERE company = :company AND is_deleted = 0")
    suspend fun getLastSyncedAt(company: String): Long?

    @Query("UPDATE tabModeOfPayment SET is_deleted = 1 WHERE is_deleted = 0 AND company = :company AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(company: String, names: List<String>)

    @Query("DELETE FROM tabModeOfPayment WHERE is_deleted = 1 AND company = :company AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(company: String, names: List<String>)

    @Query("UPDATE tabModeOfPayment SET is_deleted = 1 WHERE is_deleted = 0 AND company = :company")
    suspend fun softDeleteAllForCompany(company: String)

    @Query("DELETE FROM tabModeOfPayment WHERE is_deleted = 1 AND company = :company")
    suspend fun hardDeleteAllDeletedForCompany(company: String)
}
