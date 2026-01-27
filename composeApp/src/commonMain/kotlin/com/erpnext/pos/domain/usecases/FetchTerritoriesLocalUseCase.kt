package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.TerritoryRepository
import com.erpnext.pos.domain.models.TerritoryBO

class FetchTerritoriesLocalUseCase(
    private val repository: TerritoryRepository
) {
    suspend operator fun invoke(): List<TerritoryBO> {
        return repository.getLocalTerritories()
    }
}
