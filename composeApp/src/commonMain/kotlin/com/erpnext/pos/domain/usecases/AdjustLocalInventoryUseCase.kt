package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.InventoryRepository

data class StockDelta(
    val itemCode: String,
    val qty: Double
)

data class AdjustLocalInventoryInput(
    val warehouse: String,
    val deltas: List<StockDelta>
)

class AdjustLocalInventoryUseCase(
    private val repo: InventoryRepository
) : UseCase<AdjustLocalInventoryInput, Unit>() {
    override suspend fun useCaseFunction(input: AdjustLocalInventoryInput): Unit {
        repo.decrementStock(input.warehouse, input.deltas)
    }
}
