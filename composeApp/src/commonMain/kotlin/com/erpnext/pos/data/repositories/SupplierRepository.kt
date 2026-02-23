package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.datasources.SupplierLocalSource
import com.erpnext.pos.utils.RepoTrace

class SupplierRepository(
    private val localSource: SupplierLocalSource
) {
    suspend fun getLocalSupplierNames(): List<String> {
        RepoTrace.breadcrumb("SupplierRepository", "getLocalSupplierNames")
        return localSource.getAllActive()
            .map { supplier ->
                supplier.supplierName?.trim()?.takeIf { it.isNotBlank() }
                    ?: supplier.name.trim()
            }
            .distinct()
            .sorted()
    }
}
