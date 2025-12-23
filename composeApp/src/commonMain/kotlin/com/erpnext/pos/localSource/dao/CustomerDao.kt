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
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers ORDER BY customerName ASC")
    fun getAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerName LIKE '%' || :search || '%' ORDER BY customerName ASC")
    fun getAllFiltered(search: String): Flow<List<CustomerEntity>>

    @Query(
        """
    SELECT * FROM customers
    WHERE territory = :territory
    AND (:search IS NULL OR customerName LIKE '%' || :search || '%')
    ORDER BY customerName ASC
    """
    )
    fun getByTerritory(territory: String, search: String? = null): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerName = :name")
    suspend fun getByName(name: String): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int

    @Query("SELECT * FROM customers ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldestCustomer(): CustomerEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM customers
        WHERE state = :state
        ORDER BY customerName ASC
    """
    )
    fun getByCustomerState(state: String): Flow<List<CustomerEntity>>
}