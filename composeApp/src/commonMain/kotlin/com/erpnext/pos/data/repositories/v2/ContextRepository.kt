package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ContextPullInput
import com.erpnext.pos.domain.repositories.v2.IContextRepository
import com.erpnext.pos.localSource.dao.v2.POSContextDao
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.CompanyDto
import com.erpnext.pos.remoteSource.dto.v2.EmployeeDto
import com.erpnext.pos.remoteSource.dto.v2.POSContextSnapshot
import com.erpnext.pos.remoteSource.dto.v2.POSProfileDto
import com.erpnext.pos.remoteSource.dto.v2.SalesPersonDto
import com.erpnext.pos.remoteSource.dto.v2.TerritoryDto
import com.erpnext.pos.remoteSource.dto.v2.UserDto
import com.erpnext.pos.remoteSource.mapper.v2.toEntity
import com.erpnext.pos.remoteSource.sdk.filters
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.utils.RepoTrace

class ContextRepository(
    private val posContextDao: POSContextDao,
    private val api: APIServiceV2
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

    override suspend fun getContextSnapshot(
        instanceId: String,
        companyId: String,
        userId: String,
        posProfileId: String
    ): POSContextSnapshot? {
        RepoTrace.breadcrumb("ContextRepositoryV2", "getContextSnapshot")
        val company = getCompany(instanceId, companyId) ?: return null
        val user = getUser(instanceId, companyId, userId) ?: return null
        val employee = getEmployee(instanceId, companyId, userId) ?: return null
        val salesPerson = getSalesPerson(instanceId, companyId, employee.employeeId) ?: return null
        val territory =
            getTerritory(instanceId, companyId, salesPerson.salesPersonId) ?: return null
        val posProfile = getPosProfile(instanceId, companyId, posProfileId) ?: return null
        return POSContextSnapshot(
            company = company,
            user = user,
            employee = employee,
            salesPerson = salesPerson,
            territory = territory,
            posProfile = posProfile
        )
    }

    override suspend fun pullContext(input: ContextPullInput): Boolean {
        RepoTrace.breadcrumb("ContextRepositoryV2", "pullContext")
        val company = api.list<CompanyDto>(
            doctype = ERPDocType.Company,
            filters = filters { "name" eq input.companyId }
        ).firstOrNull()

        val user = api.list<UserDto>(
            doctype = ERPDocType.User,
            filters = filters { "name" eq input.userId }
        ).firstOrNull()

        val employee = api.list<EmployeeDto>(
            doctype = ERPDocType.Employee,
            filters = filters {
                "user_id" eq input.userId
                "company" eq input.companyId
            }
        ).firstOrNull()

        val salesPerson = employee?.employeeId?.let { employeeId ->
            api.list<SalesPersonDto>(
                doctype = ERPDocType.SalesPerson,
                filters = filters { "employee" eq employeeId }
            ).firstOrNull()
        }

        val territory = salesPerson?.salesPersonId?.let { salesPersonId ->
            api.list<TerritoryDto>(
                doctype = ERPDocType.Territory,
                filters = filters { "territory_manager" eq salesPersonId }
            ).firstOrNull()
        }

        val posProfile = api.list<POSProfileDto>(
            doctype = ERPDocType.POSProfileDetails,
            filters = filters { "name" eq input.posProfileId }
        ).firstOrNull()

        var changed = false
        company?.let {
            posContextDao.upsertCompany(it.toEntity(input.companyId,input.instanceId))
            changed = true
        }
        user?.let {
            posContextDao.upsertUser(it.toEntity(input.instanceId, input.companyId))
            changed = true
        }
        employee?.let {
            posContextDao.upsertEmployee(it.toEntity(input.instanceId, input.companyId))
            changed = true
        }
        salesPerson?.let {
            posContextDao.upsertSalesPerson(it.toEntity(input.instanceId, input.companyId))
            changed = true
        }
        territory?.let {
            posContextDao.upsertTerritory(it.toEntity(input.instanceId, input.companyId))
            changed = true
        }
        posProfile?.let { profile ->
            val profileEntity = profile.toEntity(input.instanceId, input.companyId)
            val paymentEntities = profile.payments.map {
                it.toEntity(profile.posProfileId, input.instanceId, input.companyId)
            }
            posContextDao.upsertPosProfileWithPayments(profileEntity, paymentEntities)
            changed = true
        }

        return changed
    }
}
