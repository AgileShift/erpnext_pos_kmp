package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity
import com.erpnext.pos.localSource.relations.v2.CustomerWithContactsAndAddresses

@Dao
interface CustomerDao {

    @Transaction
    @Query(
        """
        SELECT * FROM customersv2
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND territoryId = :territoryId
          AND disabled = 0
          AND is_deleted = 0
    """
    )
    suspend fun getCustomersForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ): List<CustomerWithContactsAndAddresses>

    @Transaction
    @Query(
        """
        SELECT *
        FROM customersv2
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND territoryId = :territoryId
          AND disabled = 0
    """
    )
    suspend fun getCustomersWithContactsAndAddressesForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ): List<CustomerWithContactsAndAddresses>

    @Transaction
    @Query(
        """
        SELECT *
        FROM customersv2
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND customerId = :customerId
        LIMIT 1
    """
    )
    suspend fun getCustomerWithContactsAndAddresses(
        instanceId: String,
        companyId: String,
        customerId: String
    ): CustomerWithContactsAndAddresses?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<CustomerContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<CustomerAddressEntity>)

    @Transaction
    suspend fun insertCustomerWithDetails(
        customer: CustomerEntity,
        contacts: List<CustomerContactEntity>,
        addresses: List<CustomerAddressEntity>
    ) {
        insertCustomer(customer)
        if (contacts.isNotEmpty()) {
            insertContacts(contacts)
        }
        if (addresses.isNotEmpty()) {
            insertAddresses(addresses)
        }
    }

    @Transaction
    @Query(
        """
        SELECT *
        FROM customersv2
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND syncStatus = 'PENDING'
          AND is_deleted = 0
    """
    )
    suspend fun getPendingCustomersWithContactsAndAddresses(
        instanceId: String,
        companyId: String
    ): List<CustomerWithContactsAndAddresses>

    @Query(
        """
      UPDATE customersv2
      SET syncStatus = :syncStatus,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND customerId = :customerId
    """
    )
    suspend fun updateSyncStatus(
        instanceId: String,
        companyId: String,
        customerId: String,
        syncStatus: String,
        lastSyncedAt: Long?,
        updatedAt: Long
    )
}
