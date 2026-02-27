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

    @Query(
        """
        UPDATE balance_details
        SET opening_amount = opening_amount - :amount
        WHERE cashbox_id = :cashboxId
          AND mode_of_payment = :modeOfPayment
          AND opening_amount >= :amount
        """
    )
    suspend fun decreaseOpeningAmount(
        cashboxId: Long,
        modeOfPayment: String,
        amount: Double
    ): Int

    @Query(
        """
        SELECT opening_amount
        FROM balance_details
        WHERE cashbox_id = :cashboxId
          AND mode_of_payment = :modeOfPayment
        LIMIT 1
        """
    )
    suspend fun getOpeningAmountForMode(
        cashboxId: Long,
        modeOfPayment: String
    ): Double?

    @Query(
        """
        UPDATE balance_details
        SET opening_amount = opening_amount + :amount
        WHERE cashbox_id = :cashboxId
          AND mode_of_payment = :modeOfPayment
        """
    )
    suspend fun increaseOpeningAmount(
        cashboxId: Long,
        modeOfPayment: String,
        amount: Double
    ): Int

    @Query(
        """
        UPDATE balance_details
        SET opening_amount = opening_amount - :amount
        WHERE cashbox_id = :cashboxId
          AND mode_of_payment = :modeOfPayment
        """
    )
    suspend fun subtractOpeningAmount(
        cashboxId: Long,
        modeOfPayment: String,
        amount: Double
    ): Int

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

    @Transaction
    @Query("SELECT * FROM tabCashbox WHERE openingEntryId = :openingEntryId ORDER BY localId DESC LIMIT 1")
    suspend fun getByOpeningEntry(openingEntryId: String): CashboxWithDetails?

    @Transaction
    @Query("SELECT * FROM tabCashbox WHERE user = :user AND status = 1 ORDER BY localId DESC LIMIT 1")
    suspend fun getActiveEntryForUser(user: String): CashboxWithDetails?

    @Transaction
    @Query("SELECT * FROM tabCashbox WHERE status = 1")
    suspend fun getActiveCashboxes(): List<CashboxWithDetails>

    @Transaction
    @Query(
        """
        SELECT c.*
          FROM tabCashbox c
          LEFT JOIN tab_pos_closing_entry ce ON ce.name = c.closingEntryId
         WHERE c.status = 0
           AND (c.pendingSync = 1 OR ce.pending_sync = 1)
        """
    )
    suspend fun getClosedPendingSync(): List<CashboxWithDetails>

    // Marcar como synced
    @Query("UPDATE tabCashbox SET posProfile = :erpName, pendingSync = 0 WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long, erpName: String)

    @Query("UPDATE tabCashbox SET openingEntryId = :openingEntryId WHERE localId = :localId")
    suspend fun updateOpeningEntryId(localId: Long, openingEntryId: String)

    @Query("UPDATE tabCashbox SET user = :user WHERE localId = :localId")
    suspend fun updateUser(localId: Long, user: String)

    @Query("UPDATE tabCashbox SET pendingSync = :pendingSync WHERE localId = :localId")
    suspend fun updatePendingSync(localId: Long, pendingSync: Boolean)

    @Query(
        """
        UPDATE balance_details
           SET pos_opening_entry = :remoteName
         WHERE pos_opening_entry = :localName
        """
    )
    suspend fun updateBalanceDetailsOpeningEntry(localName: String, remoteName: String)

    @Query(
        """
        UPDATE balance_details
           SET pos_closing_entry = :closingName
         WHERE cashbox_id = :cashboxId
        """
    )
    suspend fun updateBalanceDetailsClosingEntry(cashboxId: Long, closingName: String)
}
