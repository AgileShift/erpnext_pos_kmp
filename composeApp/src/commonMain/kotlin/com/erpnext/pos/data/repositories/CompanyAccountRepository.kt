package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.datasources.CompanyAccountLocalSource
import com.erpnext.pos.utils.RepoTrace

class CompanyAccountRepository(
    private val localSource: CompanyAccountLocalSource
) {
    suspend fun getLocalAccountNames(): List<String> {
        RepoTrace.breadcrumb("CompanyAccountRepository", "getLocalAccountNames")
        return localSource.getAllActive()
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
}
