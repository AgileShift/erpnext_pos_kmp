package com.erpnext.pos.domain.repositories.v2

import com.erpnext.pos.localSource.entities.v2.UserEntity
import com.erpnext.pos.localSource.entities.v2.CompanyEntity
import com.erpnext.pos.localSource.entities.v2.EmployeeEntity
import com.erpnext.pos.localSource.entities.v2.SalesPersonEntity
import com.erpnext.pos.localSource.entities.v2.TerritoryEntity
import com.erpnext.pos.localSource.relations.v2.POSProfileWithPayments
import com.erpnext.pos.remoteSource.dto.v2.POSContextSnapshot

interface IContextRepository {
    suspend fun getCompany(instanceId: String, companyId: String): CompanyEntity?
    suspend fun getUser(instanceId: String, companyId: String, userId: String): UserEntity?
    suspend fun getEmployee(instanceId: String, companyId: String, userId: String): EmployeeEntity?
    suspend fun getSalesPerson(
        instanceId: String,
        companyId: String,
        employeeId: String
    ): SalesPersonEntity?

    suspend fun getTerritory(
        instanceId: String,
        companyId: String,
        salesPersonId: String
    ): TerritoryEntity?

    suspend fun getPosProfile(
        instanceId: String,
        companyId: String,
        posProfileId: String
    ) : POSProfileWithPayments?

    suspend fun getContextSnapshot(
        instanceId: String,
        companyId: String,
        userId: String,
        posProfileId: String
    ): POSContextSnapshot?

    suspend fun pullContext(input: ContextPullInput): Boolean
}
