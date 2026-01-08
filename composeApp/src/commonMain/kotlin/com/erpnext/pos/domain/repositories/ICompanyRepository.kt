package com.erpnext.pos.domain.repositories

import com.erpnext.pos.domain.models.CompanyBO

interface ICompanyRepository {
    suspend fun getCompanyInfo(): CompanyBO
    suspend fun sync(): CompanyBO
}