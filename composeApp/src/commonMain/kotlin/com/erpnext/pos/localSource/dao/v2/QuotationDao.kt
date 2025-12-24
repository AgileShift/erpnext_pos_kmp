package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.relations.v2.QuotationWithDetails

@Dao
interface QuotationDao {

    @Query(
        """
        SELECT * FROM quotations
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
    """
    )
    suspend fun getQuotations(
        instanceId: String,
        companyId: String
    ): List<QuotationEntity>

    @Query(
        """
        SELECT * FROM quotations
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND quotationId = :quotationId
          AND is_deleted = 0
        LIMIT 1
    """
    )
    suspend fun getQuotation(
        instanceId: String,
        companyId: String,
        quotationId: String
    ): QuotationEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM quotations
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND quotationId = :quotationId
          AND is_deleted = 0
        LIMIT 1
    """
    )
    suspend fun getQuotationWithDetails(
        instanceId: String,
        companyId: String,
        quotationId: String
    ): QuotationWithDetails?
}
