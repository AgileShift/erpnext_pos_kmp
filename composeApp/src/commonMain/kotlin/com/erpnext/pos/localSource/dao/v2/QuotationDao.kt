package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
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
    suspend fun getQuotationWithDetails(
        instanceId: String,
        companyId: String,
        quotationId: String
    ): QuotationWithDetails? {
        val quotation = getQuotation(instanceId, companyId, quotationId) ?: return null
        return QuotationWithDetails(
            quotation = quotation,
            items = getQuotationItems(instanceId, companyId, quotationId),
            taxes = getQuotationTaxes(instanceId, companyId, quotationId),
            customerLinks = getQuotationCustomerLinks(instanceId, companyId, quotationId)
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: QuotationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QuotationItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxes(taxes: List<QuotationTaxEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerLinks(links: List<QuotationCustomerLinkEntity>)

    @Transaction
    suspend fun insertQuotationWithDetails(
        quotation: QuotationEntity,
        items: List<QuotationItemEntity>,
        taxes: List<QuotationTaxEntity>,
        customerLinks: List<QuotationCustomerLinkEntity>
    ) {
        insertQuotation(quotation)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
        if (taxes.isNotEmpty()) {
            insertTaxes(taxes)
        }
        if (customerLinks.isNotEmpty()) {
            insertCustomerLinks(customerLinks)
        }
    }

    @Transaction
    suspend fun getPendingQuotationsWithDetails(
        instanceId: String,
        companyId: String
    ): List<QuotationWithDetails> {
        return getPendingQuotations(instanceId, companyId).map { quotation ->
            QuotationWithDetails(
                quotation = quotation,
                items = getQuotationItems(instanceId, companyId, quotation.quotationId),
                taxes = getQuotationTaxes(instanceId, companyId, quotation.quotationId),
                customerLinks = getQuotationCustomerLinks(instanceId, companyId, quotation.quotationId)
            )
        }
    }

    @Query(
        """
        SELECT * FROM quotation_items
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND quotationId = :quotationId
    """
    )
    suspend fun getQuotationItems(
        instanceId: String,
        companyId: String,
        quotationId: String
    ): List<QuotationItemEntity>

    @Query(
        """
        SELECT * FROM quotation_taxes
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND quotationId = :quotationId
    """
    )
    suspend fun getQuotationTaxes(
        instanceId: String,
        companyId: String,
        quotationId: String
    ): List<QuotationTaxEntity>

    @Query(
        """
        SELECT * FROM quotation_customer_links
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND quotationId = :quotationId
    """
    )
    suspend fun getQuotationCustomerLinks(
        instanceId: String,
        companyId: String,
        quotationId: String
    ): List<QuotationCustomerLinkEntity>

    @Query(
        """
        SELECT * FROM quotations
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
          AND syncStatus = 'PENDING'
    """
    )
    suspend fun getPendingQuotations(
        instanceId: String,
        companyId: String
    ): List<QuotationEntity>

    @Query(
        """
      UPDATE quotations
      SET syncStatus = :syncStatus,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND quotationId = :quotationId
    """
    )
    suspend fun updateSyncStatus(
        instanceId: String,
        companyId: String,
        quotationId: String,
        syncStatus: String,
        lastSyncedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
      UPDATE quotations
      SET partyName = :newCustomerId,
          customerName = :newCustomerName,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND partyName = :oldCustomerId
    """
    )
    suspend fun replaceCustomerReference(
        instanceId: String,
        companyId: String,
        oldCustomerId: String,
        newCustomerId: String,
        newCustomerName: String,
        updatedAt: Long
    )

    @Query(
        """
      UPDATE quotation_customer_links
      SET partyName = :newCustomerId,
          customerName = :newCustomerName,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND partyName = :oldCustomerId
    """
    )
    suspend fun replaceCustomerLinkReference(
        instanceId: String,
        companyId: String,
        oldCustomerId: String,
        newCustomerId: String,
        newCustomerName: String,
        updatedAt: Long
    )
}
