package com.erpnext.pos.domain.repositories.v2

interface IInventoryRepository {

    /**
     * Retorna el stock actual por item para un warehouse específico.
     * Key = itemCode / itemId
     * Value = actualQty
     */
    suspend fun getStockByWarehouse(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ): Map<String, Float>

    /**
     * Ajusta stock localmente (offline).
     * deltaQty negativo = venta
     * deltaQty positivo = ajuste / devolución
     */
    suspend fun adjustStockLocal(
        instanceId: String,
        companyId: String,
        warehouseId: String,
        itemId: String,
        deltaQty: Double
    )
}
