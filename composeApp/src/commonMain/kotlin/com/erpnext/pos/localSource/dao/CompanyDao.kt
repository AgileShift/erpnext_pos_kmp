package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.erpnext.pos.localSource.entities.CompanyEntity

@Dao
interface CompanyDao {
    @Insert()
    suspend fun insert(company: CompanyEntity)

    @Query(
        """
            SELECT * FROM companies
        """
    )
    suspend fun getCompanyInfo(): CompanyEntity?
}