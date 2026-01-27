package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers WHERE is_deleted = 0 ORDER BY customerName ASC")
    fun getAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE is_deleted = 0 AND customerName LIKE '%' || :search || '%' ORDER BY customerName ASC")
    fun getAllFiltered(search: String): Flow<List<CustomerEntity>>

    @Query(
        """
    SELECT * FROM customers
    WHERE territory = :territory
    AND is_deleted = 0
    AND (:search IS NULL OR customerName LIKE '%' || :search || '%')
    ORDER BY customerName ASC
    """
    )
    fun getByTerritory(territory: String, search: String? = null): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE is_deleted = 0 AND customerName = :name")
    suspend fun getByName(name: String): CustomerEntity?

    @Query(
        """
        UPDATE customers
        SET totalPendingAmount = :totalPendingAmount,
            pendingInvoicesCount = :pendingInvoicesCount,
            currentBalance = :currentBalance,
            availableCredit = COALESCE(:availableCredit, availableCredit),
            state = :state
        WHERE id = :customerId
          AND is_deleted = 0
        """
    )
    suspend fun updateSummary(
        customerId: String,
        totalPendingAmount: Double,
        pendingInvoicesCount: Int,
        currentBalance: Double,
        availableCredit: Double?,
        state: String
    )

    @Query("SELECT COUNT(*) FROM customers WHERE is_deleted = 0")
    suspend fun count(): Int

    @Query("SELECT * FROM customers WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldestCustomer(): CustomerEntity?

    @Query("UPDATE customers SET is_deleted = 1 WHERE is_deleted = 0 AND id NOT IN (:ids)")
    suspend fun softDeleteNotIn(ids: List<String>)

    @Query("DELETE FROM customers WHERE is_deleted = 1 AND id NOT IN (:ids)")
    suspend fun hardDeleteDeletedNotIn(ids: List<String>)

    @Query("UPDATE customers SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM customers WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query(
        """
        UPDATE customers
        SET id = :newId,
            customerName = :customerName
        WHERE id = :oldId
        """
    )
    suspend fun updateCustomerId(oldId: String, newId: String, customerName: String)

    @Transaction
    @Query(
        """
        SELECT * FROM customers
        WHERE state = :state
          AND is_deleted = 0
        ORDER BY customerName ASC
    """
    )
    fun getByCustomerState(state: String): Flow<List<CustomerEntity>>
}
