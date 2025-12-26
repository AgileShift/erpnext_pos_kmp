package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.CompanyEntity
import com.erpnext.pos.localSource.entities.v2.EmployeeEntity
import com.erpnext.pos.localSource.entities.v2.POSPaymentMethodEntity
import com.erpnext.pos.localSource.entities.v2.POSProfileEntity
import com.erpnext.pos.localSource.entities.v2.SalesPersonEntity
import com.erpnext.pos.localSource.entities.v2.TerritoryEntity
import com.erpnext.pos.localSource.entities.v2.UserEntity
import com.erpnext.pos.remoteSource.dto.v2.CompanyDto
import com.erpnext.pos.remoteSource.dto.v2.EmployeeDto
import com.erpnext.pos.remoteSource.dto.v2.POSPaymentMethodDto
import com.erpnext.pos.remoteSource.dto.v2.POSProfileDto
import com.erpnext.pos.remoteSource.dto.v2.SalesPersonDto
import com.erpnext.pos.remoteSource.dto.v2.TerritoryDto
import com.erpnext.pos.remoteSource.dto.v2.UserDto

fun CompanyDto.toEntity(companyId: String, instanceId: String) =
    CompanyEntity(
        companyName = companyName,
        abbr = abbr,
        defaultCurrency = defaultCurrency,
        country = country,
        domain = domain,
        taxId = taxId,
        isGroup = isGroup,
        parentCompanyId = parentCompanyId,
        companyLogo = companyLogo,
        letterHead = letterHead
    ).apply {
        this.companyId = companyId
        this.instanceId = instanceId
    }

fun UserDto.toEntity(instanceId: String, companyId: String) =
    UserEntity(
        userId = userId,
        email = email ?: userId,
        fullName = fullName ?: userId,
        enabled = enabled,
        userType = userType ?: "System User"
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun EmployeeDto.toEntity(instanceId: String, companyId: String) =
    EmployeeEntity(
        employeeId = employeeId,
        employeeName = employeeName,
        userId = userId ?: employeeId,
        status = status?.equals("Active", ignoreCase = true) ?: true,
        company = company
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun SalesPersonDto.toEntity(instanceId: String, companyId: String) =
    SalesPersonEntity(
        salesPersonId = salesPersonId,
        salesPersonName = salesPersonName,
        employeeId = employeeId ?: salesPersonId,
        isGroup = isGroup,
        parentSalesPersonId = parentSalesPersonId
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun TerritoryDto.toEntity(instanceId: String, companyId: String) =
    TerritoryEntity(
        territoryId = territoryId,
        territoryName = territoryName ?: territoryId,
        isGroup = isGroup,
        parentTerritoryId = parentTerritoryId,
        territoryManagerSalesPersonId = territoryManagerSalesPersonId
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun POSProfileDto.toEntity(instanceId: String, companyId: String) =
    POSProfileEntity(
        posProfileId = posProfileId,
        warehouseId = warehouseId,
        costCenterId = costCenterId,
        currency = currency,
        priceList = priceList,
        routeId = routeId,
        allowNegativeStock = allowNegativeStock,
        updateStock = updateStock,
        allowCreditSales = allowCreditSales,
        customerId = customerId,
        namingSeries = namingSeries,
        taxTemplateId = taxTemplateId,
        writeOffAccount = writeOffAccount,
        writeOffCostCenter = writeOffCostCenter,
        disabled = disabled
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun POSPaymentMethodDto.toEntity(
    posProfileId: String,
    instanceId: String,
    companyId: String
) =
    POSPaymentMethodEntity(
        posProfileId = posProfileId,
        modeOfPayment = modeOfPayment,
        isDefault = isDefault
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }
