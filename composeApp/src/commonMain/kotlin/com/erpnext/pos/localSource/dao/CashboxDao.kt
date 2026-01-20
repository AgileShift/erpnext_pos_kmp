package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CashboxWithDetails
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashboxDao {
    @Transaction
    suspend fun insert(cashbox: CashboxEntity, details: List<BalanceDetailsEntity>): Long {
        val insertId = insertEntry(cashbox)
        details.forEach { it.cashboxId = insertId }
        insertDetails(details)
        return insertId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashboxEntity): Long

    @Insert(entity = BalanceDetailsEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<BalanceDetailsEntity>)

    @Query(
        """
        UPDATE balance_details
        SET closing_amount = :closingAmount
        WHERE cashbox_id = :cashboxId
          AND mode_of_payment = :modeOfPayment
        """
    )
    suspend fun updateClosingAmount(
        cashboxId: Long,
        modeOfPayment: String,
        closingAmount: Double
    )

    @Insert(entity = POSOpeningEntryEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpeningEntry(entry: POSOpeningEntryEntity)

    // Actualizar status (e.g., al cerrar)
    @Query("UPDATE tabCashbox SET status = :status, pendingSync = :pendingSync, closingEntryId = :pceId, periodEndDate = :endDate WHERE localId = :localId")
    suspend fun updateStatus(
        localId: Long,
        status: Boolean,
        pceId: String,
        endDate: String,
        pendingSync: Boolean = true
    ): Int

    // Obtener entry activo (abierto) para user actual
    @Transaction
    @Query("SELECT * FROM tabCashbox WHERE user = :user AND status = 1 AND posProfile = :posProfile LIMIT 1")
    fun getActiveEntry(user: String, posProfile: String): Flow<CashboxWithDetails?>

    // Marcar como synced
    @Query("UPDATE tabCashbox SET posProfile = :erpName, pendingSync = 0 WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long, erpName: String)

    @Query("UPDATE tabCashbox SET openingEntryId = :openingEntryId WHERE localId = :localId")
    suspend fun updateOpeningEntryId(localId: Long, openingEntryId: String)
}
