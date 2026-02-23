package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SupplierRepository

class FetchSuppliersLocalUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(): List<String> {
        return repository.getLocalSupplierNames()
    }
}
