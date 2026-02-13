package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.CompanyBO
import com.erpnext.pos.domain.repositories.ICompanyRepository
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.CompanyDto

private fun CompanyDto.toBO(): CompanyBO {
    return CompanyBO(
        company = company,
        defaultCurrency = defaultCurrency,
        country = country,
        ruc = taxId,
    )
}

private fun CompanyDto.toEntity(): CompanyEntity {
    return CompanyEntity(
        companyName = company,
        defaultCurrency = defaultCurrency,
        country = country,
        taxId = taxId,
        isDeleted = false,
    )
}

class CompanyRepository(
    private val api: APIService,
    private val companyDao: CompanyDao,
) : ICompanyRepository {

    override suspend fun getCompanyInfo(): CompanyBO {
        val companies = api.getCompanyInfo()
        companies.forEach { companyDao.insert(it.toEntity()) }
        val names = companies.map { it.company }
        val ids = names.ifEmpty { listOf("__empty__") }
        companyDao.hardDeleteDeletedNotIn(ids)
        companyDao.softDeleteNotIn(ids)
        val first = companies.firstOrNull()
            ?: companyDao.getCompanyInfo()?.let { local ->
                CompanyDto(
                    company = local.companyName,
                    defaultCurrency = local.defaultCurrency,
                    country = local.country,
                    taxId = local.taxId
                )
            }
            ?: throw IllegalStateException("Company info not available")
        return first.toBO()
    }

    override suspend fun sync(): CompanyBO {
        return getCompanyInfo()
    }
}
