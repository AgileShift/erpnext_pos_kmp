package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CompanyAccountRepository

class FetchCompanyAccountsLocalUseCase(
    private val repository: CompanyAccountRepository
) {
    suspend operator fun invoke(): List<String> {
        return repository.getLocalAccountNames()
    }
}
