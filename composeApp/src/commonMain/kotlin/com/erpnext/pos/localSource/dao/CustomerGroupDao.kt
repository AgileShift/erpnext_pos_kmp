package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.CustomerGroupEntity

@Dao
interface CustomerGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<CustomerGroupEntity>)

    @Query("SELECT * FROM tabCustomerGroup WHERE is_deleted = 0 ORDER BY customer_group_name ASC, name ASC")
    suspend fun getAll(): List<CustomerGroupEntity>

    @Query("UPDATE tabCustomerGroup SET is_deleted = 1 WHERE is_deleted = 0 AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabCustomerGroup WHERE is_deleted = 1 AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("DELETE FROM tabCustomerGroup WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabCustomerGroup SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("SELECT * FROM tabCustomerGroup WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): CustomerGroupEntity?
}
