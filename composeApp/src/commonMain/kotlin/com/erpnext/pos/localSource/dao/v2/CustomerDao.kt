package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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
}
