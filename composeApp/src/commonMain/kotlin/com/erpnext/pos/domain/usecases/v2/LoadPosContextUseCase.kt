package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.ContextRepository
import com.erpnext.pos.remoteSource.dto.v2.POSContextSnapshot

class LoadPosContextUseCase(
    private val contextRepository: ContextRepository
) {

    suspend operator fun invoke(
        instanceId: String,
        companyId: String,
        userId: String,
        posProfileId: String
    ): POSContextSnapshot {

        val company = requireNotNull(
            contextRepository.getCompany(instanceId, companyId)
        )

        val user = requireNotNull(
            contextRepository.getUser(instanceId, companyId, userId)
        )

        val employee = requireNotNull(
            contextRepository.getEmployee(instanceId, companyId, userId)
        )

        val salesPerson = requireNotNull(
            contextRepository.getSalesPerson(
                instanceId,
                companyId,
                employee.employeeId
            )
        )

        val territory = requireNotNull(
            contextRepository.getTerritory(
                instanceId,
                companyId,
                salesPerson.salesPersonId
            )
        )

        val posProfile = requireNotNull(
            contextRepository.getPosProfile(
                instanceId,
                companyId,
                posProfileId
            )
        )

        return POSContextSnapshot(
            company = company,
            user = user,
            employee = employee,
            salesPerson = salesPerson,
            territory = territory,
            posProfile = posProfile
        )
    }
}