package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.IContextRepository
import com.erpnext.pos.localSource.dao.v2.POSContextDao

class ContextRepository(
    private val posContextDao: POSContextDao
): IContextRepository {

    override suspend fun getCompany(instanceId: String, companyId: String) =
        posContextDao.getCompany(instanceId, companyId)

    override suspend fun getUser(instanceId: String, companyId: String, userId: String) =
        posContextDao.getUser(instanceId, companyId, userId)

    override suspend fun getEmployee(instanceId: String, companyId: String, userId: String) =
        posContextDao.getEmployeeByUserId(instanceId, companyId, userId)

    override suspend fun getSalesPerson(instanceId: String, companyId: String, employeeId: String) =
        posContextDao.getSalesPersonByEmployeeId(instanceId, companyId, employeeId)

    override suspend fun getTerritory(instanceId: String, companyId: String, salesPersonId: String) =
        posContextDao.getTerritoryByManagerSalesPersonId(instanceId, companyId, salesPersonId)

    override suspend fun getPosProfile(
        instanceId: String,
        companyId: String,
        posProfileId: String
    ) = posContextDao.getPosProfileWithPayments(instanceId, companyId, posProfileId)
}