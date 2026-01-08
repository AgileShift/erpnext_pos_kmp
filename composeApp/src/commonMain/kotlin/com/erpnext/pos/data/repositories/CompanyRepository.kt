package com.erpnext.pos.data.repositories

import androidx.room.Dao
import com.erpnext.pos.domain.models.CompanyBO
import com.erpnext.pos.domain.repositories.ICompanyRepository
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.v2.CompanyDto
import com.erpnext.pos.remoteSource.mapper.v2.toEntity

fun CompanyDto.toBO(): CompanyBO {
    return CompanyBO(
        company = company,
        defaultCurrency = defaultCurrency,
        country = country,
        ruc = taxId,
    )
}

class CompanyRepository(
    private val api: APIService,
    private val companyDao: CompanyDao,
) : ICompanyRepository {
    override suspend fun getCompanyInfo(): CompanyBO {
        val company = api.getCompanyInfo().first()
        companyDao.insert(company.toEntity())

        return company.toBO()
    }

    override suspend fun sync(): CompanyBO {
        TODO("Not yet implemented")
    }
}