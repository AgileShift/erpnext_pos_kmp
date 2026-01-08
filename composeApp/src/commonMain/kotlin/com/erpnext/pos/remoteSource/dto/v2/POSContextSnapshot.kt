package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.v2.EmployeeEntity
import com.erpnext.pos.localSource.entities.v2.SalesPersonEntity
import com.erpnext.pos.localSource.entities.v2.TerritoryEntity
import com.erpnext.pos.localSource.entities.v2.UserEntity
import com.erpnext.pos.localSource.relations.v2.POSProfileWithPayments

data class POSContextSnapshot(
    val company: CompanyEntity,
    val user: UserEntity,
    val employee: EmployeeEntity,
    val salesPerson: SalesPersonEntity,
    val territory: TerritoryEntity,
    val posProfile: POSProfileWithPayments
)
