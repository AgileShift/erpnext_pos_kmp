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
    suspend fun getCustomersWithContactsAndAddressesForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ): List<CustomerWithContactsAndAddresses> {
        return getCustomersForTerritory(instanceId, companyId, territoryId).map { customer ->
            CustomerWithContactsAndAddresses(
                customer = customer,
                contacts = getContactsForCustomer(instanceId, companyId, customer.customerId),
                addresses = getAddressesForCustomer(instanceId, companyId, customer.customerId)
            )
        }
    }

    @Transaction
    suspend fun getCustomerWithContactsAndAddresses(
        instanceId: String,
        companyId: String,
        customerId: String
    ): CustomerWithContactsAndAddresses? {
        val customer = getCustomer(instanceId, companyId, customerId) ?: return null
        return CustomerWithContactsAndAddresses(
            customer = customer,
            contacts = getContactsForCustomer(instanceId, companyId, customerId),
            addresses = getAddressesForCustomer(instanceId, companyId, customerId)
        )
    }

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
    suspend fun getPendingCustomersWithContactsAndAddresses(
        instanceId: String,
        companyId: String
    ): List<CustomerWithContactsAndAddresses> {
        return getPendingCustomers(instanceId, companyId).map { customer ->
            CustomerWithContactsAndAddresses(
                customer = customer,
                contacts = getContactsForCustomer(instanceId, companyId, customer.customerId),
                addresses = getAddressesForCustomer(instanceId, companyId, customer.customerId)
            )
        }
    }

    @Query(
        """
        SELECT *
        FROM customersv2
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND customerId = :customerId
        LIMIT 1
    """
    )
    suspend fun getCustomer(
        instanceId: String,
        companyId: String,
        customerId: String
    ): CustomerEntity?

    @Query(
        """
        SELECT *
        FROM customer_contact
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND customerId = :customerId
    """
    )
    suspend fun getContactsForCustomer(
        instanceId: String,
        companyId: String,
        customerId: String
    ): List<CustomerContactEntity>

    @Query(
        """
        SELECT *
        FROM customer_address
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND customerId = :customerId
    """
    )
    suspend fun getAddressesForCustomer(
        instanceId: String,
        companyId: String,
        customerId: String
    ): List<CustomerAddressEntity>

    @Query(
        """
        SELECT *
        FROM customersv2
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND syncStatus = 'PENDING'
          AND is_deleted = 0
    """
    )
    suspend fun getPendingCustomers(
        instanceId: String,
        companyId: String
    ): List<CustomerEntity>

    @Query(
        """
        SELECT *
        FROM customersv2
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND syncStatus = 'SYNCED'
          AND is_deleted = 0
          AND (
                LOWER(customerName) = LOWER(:customerName)
                OR (:mobileNo IS NOT NULL AND mobileNo = :mobileNo)
              )
        LIMIT 1
    """
    )
    suspend fun findSyncedCustomerMatch(
        instanceId: String,
        companyId: String,
        customerName: String,
        mobileNo: String?
    ): CustomerEntity?

    @Query(
        """
        SELECT remote_name
        FROM customersv2
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND customerId = :customerId
        LIMIT 1
    """
    )
    suspend fun getRemoteCustomerId(
        instanceId: String,
        companyId: String,
        customerId: String
    ): String?

    @Query(
        """
      UPDATE customersv2
      SET syncStatus = 'SYNCED',
          remote_name = :remoteName,
          remote_modified = :remoteModified,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt,
          is_deleted = :isDeleted
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND customerId = :customerId
    """
    )
    suspend fun markMergedCustomer(
        instanceId: String,
        companyId: String,
        customerId: String,
        remoteName: String,
        remoteModified: String?,
        lastSyncedAt: Long,
        updatedAt: Long,
        isDeleted: Boolean
    )

    @Query(
        """
      UPDATE customersv2
      SET syncStatus = 'SYNCED',
          remote_name = :remoteName,
          remote_modified = :remoteModified,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND customerId = :customerId
    """
    )
    suspend fun markCustomerSynced(
        instanceId: String,
        companyId: String,
        customerId: String,
        remoteName: String,
        remoteModified: String?,
        lastSyncedAt: Long,
        updatedAt: Long
    )

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
