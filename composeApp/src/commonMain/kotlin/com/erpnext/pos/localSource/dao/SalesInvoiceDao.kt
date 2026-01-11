package com.erpnext.pos.localSource.dao

import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.room.*
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments

@Dao
interface SalesInvoiceDao {

    // 🔹 Inserciones
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SalesInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SalesInvoiceItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<POSInvoicePaymentEntity>)

    @Query("SELECT * FROM tabSalesInvoicePayment WHERE parent_invoice = :invoiceName")
    suspend fun getPaymentsForInvoice(invoiceName: String): List<POSInvoicePaymentEntity>

    @Transaction
    suspend fun insertFullInvoices(invoices: List<SalesInvoiceWithItemsAndPayments>) {
        invoices.map { payload ->
            val existingPayments = if (payload.payments.isEmpty()) {
                getPaymentsForInvoice(payload.invoice.invoiceName.orEmpty())
            } else {
                emptyList()
            }
            insertInvoice(payload.invoice)
            insertItems(payload.items)
            when {
                payload.payments.isNotEmpty() -> insertPayments(payload.payments)
                existingPayments.isNotEmpty() -> insertPayments(existingPayments)
            }
        }
    }

    // 🔹 Inserción transaccional completa
    @Transaction
    suspend fun insertFullInvoice(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity> = emptyList()
    ) {
        val existingPayments = if (payments.isEmpty()) {
            getPaymentsForInvoice(invoice.invoiceName.orEmpty())
        } else {
            emptyList()
        }
        insertInvoice(invoice)
        insertItems(items)
        when {
            payments.isNotEmpty() -> insertPayments(payments)
            existingPayments.isNotEmpty() -> insertPayments(existingPayments)
        }
    }

    // 🔹 Consultas
    @Transaction
    @Query("SELECT * FROM tabSalesInvoice ORDER BY posting_date DESC")
    fun getAllInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice ORDER BY posting_date DESC")
    suspend fun getAllInvoices(): List<SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE invoice_name = :invoiceName LIMIT 1")
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE sync_status = 'Pending'")
    suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments>

    @Query("UPDATE tabSalesInvoice SET sync_status = :status WHERE invoice_name = :invoiceName")
    suspend fun updateSyncStatus(invoiceName: String, status: String)

    // 🔹 Métricas financieras
    @Query("SELECT SUM(grand_total) FROM tabSalesInvoice WHERE posting_date = :date AND docstatus = 1")
    suspend fun getTotalSalesForDate(date: String): Double?

    @Query("SELECT COUNT(*) FROM tabSalesInvoice WHERE posting_date = :date AND docstatus = 1")
    suspend fun getSalesCountForDate(date: String): Int

    @Query("SELECT COUNT(DISTINCT customer) FROM tabSalesInvoice WHERE posting_date = :date AND docstatus = 1")
    suspend fun getDistinctCustomersForDate(date: String): Int

    @Query(
        """
        SELECT posting_date AS date, SUM(grand_total) AS total
        FROM tabSalesInvoice
        WHERE posting_date BETWEEN :startDate AND :endDate
          AND docstatus = 1
        GROUP BY posting_date
        ORDER BY posting_date ASC
        """
    )
    suspend fun getDailySalesTotals(
        startDate: String,
        endDate: String
    ): List<DailySalesTotal>

    @Query(
        """
        SELECT i.item_code AS itemCode,
               i.item_name AS itemName,
               SUM(i.qty) AS qty,
               SUM(i.amount) AS total
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
        GROUP BY i.item_code, i.item_name
        ORDER BY total DESC
        LIMIT :limit
        """
    )
    suspend fun getTopProductsBySales(
        startDate: String,
        endDate: String,
        limit: Int
    ): List<TopProductSales>

    @Query(
        """
        SELECT SUM(
            (CASE WHEN i.net_amount > 0 THEN i.net_amount ELSE i.amount END)
            - (IFNULL(t.valuation_rate, 0) * i.qty)
        )
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        LEFT JOIN tabItem t ON t.itemCode = i.item_code
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
        """
    )
    suspend fun getEstimatedMarginTotal(
        startDate: String,
        endDate: String
    ): Double?

    @Query(
        """
        SELECT COUNT(*)
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        LEFT JOIN tabItem t ON t.itemCode = i.item_code
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND IFNULL(t.valuation_rate, 0) > 0
        """
    )
    suspend fun countItemsWithCost(
        startDate: String,
        endDate: String
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
        """
    )
    suspend fun countItemsInRange(
        startDate: String,
        endDate: String
    ): Int

    @Query(
        """
        SELECT i.item_code AS itemCode,
               i.item_name AS itemName,
               SUM(i.qty) AS qty,
               SUM(CASE WHEN i.net_amount > 0 THEN i.net_amount ELSE i.amount END) AS total,
               SUM(
                    (CASE WHEN i.net_amount > 0 THEN i.net_amount ELSE i.amount END)
                    - (IFNULL(t.valuation_rate, 0) * i.qty)
               ) AS margin
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        LEFT JOIN tabItem t ON t.itemCode = i.item_code
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND IFNULL(t.valuation_rate, 0) > 0
        GROUP BY i.item_code, i.item_name
        ORDER BY margin DESC
        LIMIT :limit
        """
    )
    suspend fun getTopProductsByMargin(
        startDate: String,
        endDate: String,
        limit: Int
    ): List<TopProductMargin>

    @Query("SELECT SUM(outstanding_amount) FROM tabSalesInvoice WHERE status IN ('Draft','Submitted')")
    suspend fun getTotalOutstanding(): Double?

    // 🔹 Limpieza / control
    @Query("DELETE FROM tabSalesInvoice WHERE docstatus = 2") // Cancelled
    suspend fun deleteCancelledInvoices()

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE customer_name LIKE '%' || :search || '%' ORDER BY customer_name ASC")
    fun getAllFiltered(search: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query(
        """
        SELECT * FROM tabSalesInvoice
        WHERE customer = :customerName
          AND outstanding_amount > 0
        ORDER BY posting_date DESC
        """
    )
    suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE posting_date BETWEEN :startDate AND :endDate ORDER BY posting_date DESC")
    fun getInvoicesByDateRange(
        startDate: String,
        endDate: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE due_date < :today AND outstanding_amount > 0 AND status IN ('Overdue', 'Unpaid', 'Partly Paid') ORDER BY due_date ASC")
    fun getOverdueInvoices(today: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE posting_date BETWEEN :startDate AND :endDate AND due_date < :today AND outstanding_amount > 0 ORDER BY due_date ASC")
    fun getOverdueInvoicesInRange(
        startDate: String, endDate: String, today: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query(
        """
        SELECT * FROM tabSalesInvoice 
        WHERE (:query IS NULL OR customer_name LIKE '%' || :query || '%' OR invoice_name LIKE '%' || :query || '%')
        AND ((:date IS NULL OR posting_date == :date)) 
        AND status IN ('Unpaid', 'Overdue', 'Partly Paid')
        AND outstanding_amount > 0
        ORDER BY posting_date DESC 
    """
    )
    fun getFilteredInvoices(
        query: String?,
        date: String?,
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Update
    suspend fun updateInvoice(invoice: SalesInvoiceEntity)

    @Query("""
       UPDATE tabSalesInvoice SET status = :status WHERE invoice_name = :invoiceId
    """)
    suspend fun updatePaymentStatus(invoiceId: String, status: String)

    @Transaction
    suspend fun applyPayments(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    ) {
        updateInvoice(invoice)
        insertPayments(payments)
    }

    @Query("SELECT * FROM tabSalesInvoicePayment WHERE sync_status != 'Synced'")
    suspend fun getPendingPayments(): List<POSInvoicePaymentEntity>

    @Query(
        """
        UPDATE tabSalesInvoicePayment
        SET sync_status = :status,
            last_synced_at = :syncedAt
        WHERE id = :paymentId
        """
    )
    suspend fun updatePaymentSyncStatus(paymentId: Int, status: String, syncedAt: Long)

    @Query(
        """
        UPDATE customers
        SET totalPendingAmount = COALESCE(
                (SELECT SUM(outstanding_amount)
                 FROM tabSalesInvoice
                 WHERE customer = :customerId
                   AND outstanding_amount > 0
                   AND docstatus != 2), 0
            ),
            pendingInvoicesCount = COALESCE(
                (SELECT COUNT(*)
                 FROM tabSalesInvoice
                 WHERE customer = :customerId
                   AND outstanding_amount > 0
                   AND docstatus != 2), 0
            ),
            currentBalance = COALESCE(
                (SELECT SUM(outstanding_amount)
                 FROM tabSalesInvoice
                 WHERE customer = :customerId
                   AND outstanding_amount > 0
                   AND docstatus != 2), 0
            ),
            availableCredit = CASE
                WHEN creditLimit IS NULL THEN availableCredit
                ELSE creditLimit - COALESCE(
                    (SELECT SUM(outstanding_amount)
                     FROM tabSalesInvoice
                     WHERE customer = :customerId
                       AND outstanding_amount > 0
                       AND docstatus != 2), 0
                )
            END,
            state = CASE
                WHEN COALESCE(
                    (SELECT SUM(outstanding_amount)
                     FROM tabSalesInvoice
                     WHERE customer = :customerId
                       AND outstanding_amount > 0
                       AND docstatus != 2), 0
                ) > 0 THEN 'Pendientes'
                ELSE 'Sin Pendientes'
            END
        WHERE id = :customerId
        """
    )
    suspend fun refreshCustomerSummary(customerId: String)

    @Query("SELECT COUNT(*) FROM tabSalesInvoice")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM tabSalesInvoice WHERE sync_status = 'Pending'")
    suspend fun countAllSyncPending(): Int

    @Query("DELETE FROM tabSalesInvoice WHERE invoice_name =:invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: String)

    @Query("DELETE FROM tabSalesInvoice")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE tabSalesInvoice
        SET invoice_name = :newName,
            customer_name = :customerName,
            customer_phone = :customerPhone,
            posting_date = :postingDate,
            due_date = :dueDate,
            currency = :currency,
            party_account_currency = :partyAccountCurrency,
            conversion_rate = :conversionRate,
            custom_exchange_rate = :customExchangeRate,
            net_total = :netTotal,
            tax_total = :taxTotal,
            grand_total = :grandTotal,
            paid_amount = :paidAmount,
            outstanding_amount = :outstandingAmount,
            status = :status,
            docstatus = :docstatus,
            mode_of_payment = :modeOfPayment,
            debit_to = :debitTo,
            remarks = :remarks,
            sync_status = :syncStatus,
            modified_at = :modifiedAt
        WHERE invoice_name = :oldName
        """
    )
    suspend fun updateFromRemote(
        oldName: String,
        newName: String,
        customerName: String?,
        customerPhone: String?,
        postingDate: String,
        dueDate: String?,
        currency: String,
        partyAccountCurrency: String?,
        conversionRate: Double?,
        customExchangeRate: Double?,
        netTotal: Double,
        taxTotal: Double,
        grandTotal: Double,
        paidAmount: Double,
        outstandingAmount: Double,
        status: String,
        docstatus: Int,
        modeOfPayment: String?,
        debitTo: String?,
        remarks: String?,
        syncStatus: String,
        modifiedAt: Long
    )

    @Query("SELECT * FROM tabSalesInvoice ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldestItem(): SalesInvoiceEntity?
}

data class DailySalesTotal(
    val date: String,
    val total: Double
)

data class TopProductSales(
    val itemCode: String,
    val itemName: String?,
    val qty: Double,
    val total: Double
)

data class TopProductMargin(
    val itemCode: String,
    val itemName: String?,
    val qty: Double,
    val total: Double,
    val margin: Double
)
