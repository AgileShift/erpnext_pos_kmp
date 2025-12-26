package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.v2.CustomerAgingRow

@Dao
interface SalesInvoiceDao {

    @Query(
        """
        SELECT * FROM sales_invoices
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND customerId = :customerId
          AND is_deleted = 0
          AND status IN ('Unpaid', 'Overdue')
    """
    )
    suspend fun getOpenInvoicesForCustomer(
        instanceId: String,
        companyId: String,
        customerId: String
    ): List<SalesInvoiceEntity>

    @Query(
        """
        SELECT * FROM sales_invoices
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND invoiceId = :invoiceId
          AND is_deleted = 0
    """
    )
    suspend fun getInvoice(
        instanceId: String,
        companyId: String,
        invoiceId: String
    ): SalesInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SalesInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SalesInvoiceItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<SalesInvoicePaymentEntity>)

    @Transaction
    suspend fun insertInvoiceWithItemsAndPayments(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<SalesInvoicePaymentEntity>
    ) {
        insertInvoice(invoice)
        //TODO: Deberia validar desde UI, no se puede crear factura sin items
        if (items.isNotEmpty())
            insertItems(items)
        if (payments.isNotEmpty())
            insertPayments(payments)
    }

    @Transaction
    suspend fun getPendingInvoicesWithDetails(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return getPendingInvoices(instanceId, companyId).map { invoice ->
            SalesInvoiceWithItemsAndPayments(
                invoice = invoice,
                items = getInvoiceItems(instanceId, companyId, invoice.invoiceId),
                payments = getInvoicePayments(instanceId, companyId, invoice.invoiceId)
            )
        }
    }

    @Query(
        """
        SELECT * FROM sales_invoices
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND syncStatus = 'PENDING'
          AND is_deleted = 0
    """
    )
    suspend fun getPendingInvoices(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceEntity>

    @Update
    suspend fun updateInvoice(invoice: SalesInvoiceEntity)

    @Transaction
    suspend fun applyPayments(
        invoice: SalesInvoiceEntity,
        payments: List<SalesInvoicePaymentEntity>
    ) {
        insertPayments(payments)
        updateInvoice(invoice)
    }

    @Transaction
    suspend fun getInvoicesForRoute(
        instanceId: String,
        companyId: String,
        territoryId: String,
        fromDate: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return getInvoiceHeadersForRoute(
            instanceId,
            companyId,
            territoryId,
            fromDate
        ).map { invoice ->
            SalesInvoiceWithItemsAndPayments(
                invoice = invoice,
                items = getInvoiceItems(instanceId, companyId, invoice.invoiceId),
                payments = getInvoicePayments(instanceId, companyId, invoice.invoiceId)
            )
        }
    }

    @Query(
        """
      SELECT 
        customerId,
        SUM(outstandingAmount) AS outstanding,
        SUM(CASE WHEN status = 'Overdue' OR status = 'Unpaid' THEN outstandingAmount ELSE 0 END) AS overdue,
        MAX(postingDate) AS lastPurchaseDate
      FROM sales_invoices
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND territoryId = :territoryId
        AND is_deleted = 0
        AND status IN ('Unpaid', 'Overdue')
      GROUP BY customerId
    """
    )
    suspend fun getCustomerAgingForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ): List<CustomerAgingRow>

    @Query(
        """
      SELECT COUNT(*) FROM sales_invoices
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND is_deleted = 0
        AND syncStatus = 'PENDING'
    """
    )
    suspend fun countPendingInvoices(instanceId: String, companyId: String): Int

    @Query(
        """
      SELECT COUNT(*) FROM sales_invoices
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND is_deleted = 0
        AND syncStatus = 'FAILED'
    """
    )
    suspend fun countFailedInvoices(instanceId: String, companyId: String): Int

    @Query(
        """
      UPDATE sales_invoices
      SET syncStatus = :syncStatus,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND invoiceId = :invoiceId
    """
    )
    suspend fun updateSyncStatus(
        instanceId: String,
        companyId: String,
        invoiceId: String,
        syncStatus: String,
        lastSyncedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
      UPDATE sales_invoices
      SET customerId = :newCustomerId,
          customerName = :newCustomerName,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND customerId = :oldCustomerId
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
        SELECT *
        FROM sales_invoices
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND territoryId = :territoryId
          AND docStatus = 1
          AND (
                postingDate >= :fromDateInclusive
                OR status IN ('Unpaid', 'Overdue')
              )
        ORDER BY postingDate DESC
    """
    )
    suspend fun getRelevantInvoicesForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String,
        fromDateInclusive: Long
    ): List<SalesInvoiceEntity>

    // ------------- OUTBOX SYNC -------------
    @Transaction
    suspend fun getPendingOutbox(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return getPendingOutboxHeaders(instanceId, companyId).map { invoice ->
            SalesInvoiceWithItemsAndPayments(
                invoice = invoice,
                items = getInvoiceItems(instanceId, companyId, invoice.invoiceId),
                payments = getInvoicePayments(instanceId, companyId, invoice.invoiceId)
            )
        }
    }

    @Query(
        """
            UPDATE sales_invoices
            SET syncStatus = 'SYNCED',
                remote_name = :remoteName,
                remote_modified = :remoteModified,
                lastSyncedAt = :syncedAd,
                updated_at = :syncedAd
            WHERE instanceId = :instanceId 
                AND companyId = :companyId 
                AND invoiceId = :localInvoiceId
        """
    )
    suspend fun markSynced(
        instanceId: String,
        companyId: String,
        localInvoiceId: String,
        remoteName: String,
        remoteModified: String?,
        syncedAd: Long
    )

    @Query(
        """
        SELECT remote_modified
        FROM sales_invoices
        WHERE instanceId = :instanceId
            AND companyId = :companyId
            AND (remote_name = :remoteName OR invoiceId = :remoteName)
        LIMIT 1
    """
    )
    suspend fun getRemoteModified(
        instanceId: String,
        companyId: String,
        remoteName: String
    ): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoices(list: List<SalesInvoiceEntity>)

    @Query(
        """
        SELECT * FROM sales_invoice_items
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND invoiceId = :invoiceId
    """
    )
    suspend fun getInvoiceItems(
        instanceId: String,
        companyId: String,
        invoiceId: String
    ): List<SalesInvoiceItemEntity>

    @Query(
        """
        SELECT * FROM sales_invoice_payments
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND invoiceId = :invoiceId
    """
    )
    suspend fun getInvoicePayments(
        instanceId: String,
        companyId: String,
        invoiceId: String
    ): List<SalesInvoicePaymentEntity>

    @Query(
        """
        SELECT * FROM sales_invoices
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND territoryId = :territoryId
          AND is_deleted = 0
          AND (
                syncStatus = 'PENDING'
                OR postingDate >= :fromDate
              )
    """
    )
    suspend fun getInvoiceHeadersForRoute(
        instanceId: String,
        companyId: String,
        territoryId: String,
        fromDate: String
    ): List<SalesInvoiceEntity>

    @Query(
        """
        SELECT *
        FROM sales_invoices
        WHERE instanceId = :instanceId 
            AND companyId = :companyId
            AND (syncStatus IS NULL OR syncStatus = 'SYNCED')
            AND is_deleted = 0
        ORDER BY created_at ASC
    """
    )
    suspend fun getPendingOutboxHeaders(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceEntity>
}
