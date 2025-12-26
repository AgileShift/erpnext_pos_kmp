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

    @Transaction
    suspend fun insertFullInvoices(invoices: List<SalesInvoiceWithItemsAndPayments>) {
        invoices.map {
            insertInvoice(it.invoice)
            insertItems(it.items)
            if (it.payments.isNotEmpty()) insertPayments(it.payments)
        }
    }

    // 🔹 Inserción transaccional completa
    @Transaction
    suspend fun insertFullInvoice(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity> = emptyList()
    ) {
        insertInvoice(invoice)
        insertItems(items)
        if (payments.isNotEmpty()) insertPayments(payments)
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

    @Query("SELECT SUM(outstanding_amount) FROM tabSalesInvoice WHERE status IN ('Draft','Submitted')")
    suspend fun getTotalOutstanding(): Double?

    // 🔹 Limpieza / control
    @Query("DELETE FROM tabSalesInvoice WHERE docstatus = 2") // Cancelled
    suspend fun deleteCancelledInvoices()

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE customer_name LIKE '%' || :search || '%' ORDER BY customer_name ASC")
    fun getAllFiltered(search: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE posting_date BETWEEN :startDate AND :endDate ORDER BY posting_date DESC")
    fun getInvoicesByDateRange(
        startDate: String,
        endDate: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE due_date < :today AND outstanding_amount > 0 AND status IN ('Overdue', 'Unpaid') ORDER BY due_date ASC")
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
        ORDER BY posting_date DESC 
    """
    )
    fun getFilteredInvoices(
        query: String?,
        date: String?,
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Query("SELECT COUNT(*) FROM tabSalesInvoice")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM tabSalesInvoice WHERE sync_status = 'Pending'")
    suspend fun countAllSyncPending(): Int

    @Query("DELETE FROM tabSalesInvoice WHERE invoice_name =:invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: String)

    @Query("DELETE FROM tabSalesInvoice")
    suspend fun deleteAll()

    @Query("SELECT * FROM tabSalesInvoice ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldestItem(): SalesInvoiceEntity?
}
