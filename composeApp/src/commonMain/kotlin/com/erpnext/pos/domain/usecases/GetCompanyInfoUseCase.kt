package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CompanyRepository
import com.erpnext.pos.domain.models.CompanyBO

class GetCompanyInfoUseCase(
    private val repo: CompanyRepository
) : UseCase<Unit?, CompanyBO>() {
    override suspend fun useCaseFunction(input: Unit?): CompanyBO {
        return repo.getCompanyInfo()
    }
}