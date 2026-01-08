package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.v2.EmployeeEntity
import com.erpnext.pos.localSource.entities.v2.POSPaymentMethodEntity
import com.erpnext.pos.localSource.entities.v2.POSProfileEntity
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
        /*WHERE instanceId = :instanceId AND companyId = :companyId*/
        LIMIT 1
    """
    )
    suspend fun getCompany(/*instanceId: String, companyId: String*/): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompany(company: CompanyEntity)

    @Query(
        """
        SELECT *
        FROM users
        WHERE instanceId = :instanceId AND companyId = :companyId AND userId = :userId
        LIMIT 1
    """
    )
    suspend fun getUser(instanceId: String, companyId: String, userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployee(employee: EmployeeEntity)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalesPerson(salesPerson: SalesPersonEntity)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTerritory(territory: TerritoryEntity)

    @Transaction
    suspend fun getPosProfileWithPayments(
        instanceId: String,
        companyId: String,
        posProfileId: String
    ): POSProfileWithPayments? {
        val profile = getPosProfile(instanceId, companyId, posProfileId) ?: return null
        return POSProfileWithPayments(
            profile = profile,
            paymentMethods = getPaymentMethods(instanceId, companyId, posProfileId)
        )
    }

    @Query(
        """
        SELECT *
        FROM pos_profiles
        WHERE instanceId = :instanceId AND companyId = :companyId AND posProfileId = :posProfileId
        LIMIT 1
    """
    )
    suspend fun getPosProfile(
        instanceId: String,
        companyId: String,
        posProfileId: String
    ): POSProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosProfile(profile: POSProfileEntity)

    @Query(
        """
        SELECT *
        FROM pos_payment_methods
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND posProfileId = :posProfileId
    """
    )
    suspend fun getPaymentMethods(
        instanceId: String,
        companyId: String,
        posProfileId: String
    ): List<POSPaymentMethodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentMethods(methods: List<POSPaymentMethodEntity>)

    @Transaction
    suspend fun upsertPosProfileWithPayments(
        profile: POSProfileEntity,
        paymentMethods: List<POSPaymentMethodEntity>
    ) {
        upsertPosProfile(profile)
        if (paymentMethods.isNotEmpty()) {
            upsertPaymentMethods(paymentMethods)
        }
    }
}
