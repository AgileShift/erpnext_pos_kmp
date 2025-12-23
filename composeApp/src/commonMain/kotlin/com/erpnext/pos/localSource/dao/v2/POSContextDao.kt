package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.CompanyEntity
import com.erpnext.pos.localSource.entities.v2.EmployeeEntity
import com.erpnext.pos.localSource.entities.v2.SalesPersonEntity
import com.erpnext.pos.localSource.entities.v2.TerritoryEntity
import com.erpnext.pos.localSource.entities.v2.UserEntity
import com.erpnext.pos.localSource.relations.v2.POSProfileWithPayments

@Dao
interface POSContextDao {

    @Query(
        """
        SELECT * 
        FROM companies
        WHERE instanceId = :instanceId AND companyId = :companyId
        LIMIT 1
    """
    )
    suspend fun getCompany(instanceId: String, companyId: String): CompanyEntity?

    @Query(
        """
        SELECT *
        FROM users
        WHERE instanceId = :instanceId AND companyId = :companyId AND userId = :userId
        LIMIT 1
    """
    )
    suspend fun getUser(instanceId: String, companyId: String, userId: String): UserEntity?

    @Query(
        """
        SELECT *
        FROM employees
        WHERE instanceId = :instanceId AND companyId = :companyId AND userId = :userId
        LIMIT 1
    """
    )
    suspend fun getEmployeeByUserId(
        instanceId: String,
        companyId: String,
        userId: String
    ): EmployeeEntity?

    @Query(
        """
        SELECT *
        FROM sales_people
        WHERE instanceId = :instanceId AND companyId = :companyId AND employeeId = :employeeId
        LIMIT 1
    """
    )
    suspend fun getSalesPersonByEmployeeId(
        instanceId: String,
        companyId: String,
        employeeId: String
    ): SalesPersonEntity?

    @Query(
        """
        SELECT *
        FROM territories
        WHERE instanceId = :instanceId AND companyId = :companyId AND territoryManagerSalesPersonId = :salesPersonId
        LIMIT 1
    """
    )
    suspend fun getTerritoryByManagerSalesPersonId(
        instanceId: String,
        companyId: String,
        salesPersonId: String
    ): TerritoryEntity?

    @Transaction
    @Query(
        """
        SELECT *
        FROM pos_profiles
        WHERE instanceId = :instanceId AND companyId = :companyId AND posProfileId = :posProfileId
        LIMIT 1
    """
    )
    suspend fun getPosProfileWithPayments(
        instanceId: String,
        companyId: String,
        posProfileId: String
    ): POSProfileWithPayments?
}