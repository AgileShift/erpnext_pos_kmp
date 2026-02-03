package com.erpnext.pos.localSource.dao

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
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 ORDER BY posting_date DESC")
    fun getAllInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 ORDER BY posting_date DESC")
    suspend fun getAllInvoices(): List<SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 AND invoice_name = :invoiceName LIMIT 1")
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?

    @Query(
        """
        SELECT i.invoice_name
        FROM tabSalesInvoice i
        LEFT JOIN tabSalesInvoiceItem it
          ON it.parent_invoice = i.invoice_name
        WHERE i.is_deleted = 0
          AND i.profile_id = :profileId
        GROUP BY i.invoice_name
        HAVING COUNT(it.id) = 0
        LIMIT :limit
        """
    )
    suspend fun getInvoiceNamesMissingItems(profileId: String, limit: Int = 50): List<String>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 AND sync_status = 'Pending'")
    suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments>

    @Query("UPDATE tabSalesInvoice SET sync_status = :status WHERE invoice_name = :invoiceName AND is_deleted = 0")
    suspend fun updateSyncStatus(invoiceName: String, status: String)

    // 🔹 Métricas financieras
    @Query("SELECT SUM(grand_total) FROM tabSalesInvoice WHERE posting_date = :date AND docstatus = 1 AND is_deleted = 0")
    suspend fun getTotalSalesForDate(date: String): Double?

    @Query(
        """
        SELECT DISTINCT currency
        FROM tabSalesInvoice
        WHERE currency IS NOT NULL AND currency != ''
          AND is_deleted = 0
        """
    )
    suspend fun getAvailableCurrencies(): List<String>

    @Query(
        """
        SELECT currency AS currency,
               SUM(grand_total) AS total,
               COUNT(*) AS invoices,
               COUNT(DISTINCT customer) AS customers
        FROM tabSalesInvoice
        WHERE posting_date = :date
          AND docstatus = 1
          AND is_deleted = 0
        GROUP BY currency
        """
    )
    suspend fun getSalesSummaryForDateByCurrency(date: String): List<CurrencySalesSummary>

    @Query("SELECT COUNT(*) FROM tabSalesInvoice WHERE posting_date = :date AND docstatus = 1 AND is_deleted = 0")
    suspend fun getSalesCountForDate(date: String): Int

    @Query("SELECT COUNT(DISTINCT customer) FROM tabSalesInvoice WHERE posting_date = :date AND docstatus = 1 AND is_deleted = 0")
    suspend fun getDistinctCustomersForDate(date: String): Int

    @Query(
        """
        SELECT posting_date AS date, SUM(grand_total) AS total
        FROM tabSalesInvoice
        WHERE posting_date BETWEEN :startDate AND :endDate
          AND docstatus = 1
          AND is_deleted = 0
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
        SELECT posting_date AS date,
               currency AS currency,
               SUM(grand_total) AS total
        FROM tabSalesInvoice
        WHERE posting_date BETWEEN :startDate AND :endDate
          AND docstatus = 1
          AND is_deleted = 0
        GROUP BY posting_date, currency
        ORDER BY posting_date ASC
        """
    )
    suspend fun getDailySalesTotalsByCurrency(
        startDate: String,
        endDate: String
    ): List<CurrencyDailySalesTotal>

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
          AND s.is_deleted = 0
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
          AND s.is_deleted = 0
        """
    )
    suspend fun getEstimatedMarginTotal(
        startDate: String,
        endDate: String
    ): Double?

    @Query(
        """
        SELECT s.currency AS currency,
               SUM(
                    (CASE WHEN i.net_amount > 0 THEN i.net_amount ELSE i.amount END)
                    - (IFNULL(t.valuation_rate, 0) * i.qty)
               ) AS margin
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        LEFT JOIN tabItem t ON t.itemCode = i.item_code
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND s.is_deleted = 0
        GROUP BY s.currency
        """
    )
    suspend fun getEstimatedMarginTotalByCurrency(
        startDate: String,
        endDate: String
    ): List<CurrencyMarginTotal>

    @Query(
        """
        SELECT COUNT(*)
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        LEFT JOIN tabItem t ON t.itemCode = i.item_code
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND s.is_deleted = 0
          AND IFNULL(t.valuation_rate, 0) > 0
        """
    )
    suspend fun countItemsWithCost(
        startDate: String,
        endDate: String
    ): Int

    @Query(
        """
        SELECT s.currency AS currency, COUNT(*) AS count
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        LEFT JOIN tabItem t ON t.itemCode = i.item_code
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND s.is_deleted = 0
          AND IFNULL(t.valuation_rate, 0) > 0
        GROUP BY s.currency
        """
    )
    suspend fun countItemsWithCostByCurrency(
        startDate: String,
        endDate: String
    ): List<CurrencyItemCount>

    @Query(
        """
        SELECT COUNT(*)
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND s.is_deleted = 0
        """
    )
    suspend fun countItemsInRange(
        startDate: String,
        endDate: String
    ): Int

    @Query(
        """
        SELECT s.currency AS currency, COUNT(*) AS count
        FROM tabSalesInvoiceItem i
        INNER JOIN tabSalesInvoice s ON s.invoice_name = i.parent_invoice
        WHERE s.posting_date BETWEEN :startDate AND :endDate
          AND s.docstatus = 1
          AND s.is_deleted = 0
        GROUP BY s.currency
        """
    )
    suspend fun countItemsInRangeByCurrency(
        startDate: String,
        endDate: String
    ): List<CurrencyItemCount>

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
          AND s.is_deleted = 0
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

    @Query("SELECT SUM(outstanding_amount) FROM tabSalesInvoice WHERE status IN ('Draft','Submitted') AND is_deleted = 0")
    suspend fun getTotalOutstanding(): Double?

    @Query(
        """
        SELECT currency AS currency,
               SUM(outstanding_amount) AS total
        FROM tabSalesInvoice
        WHERE status IN ('Draft','Submitted')
          AND is_deleted = 0
        GROUP BY currency
        """
    )
    suspend fun getOutstandingTotalsByCurrency(): List<CurrencyOutstandingTotal>

    // 🔹 Limpieza / control
    @Query("DELETE FROM tabSalesInvoice WHERE docstatus = 2") // Cancelled
    suspend fun deleteCancelledInvoices()

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 AND customer_name LIKE '%' || :search || '%' ORDER BY customer_name ASC")
    fun getAllFiltered(search: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query(
        """
        SELECT * FROM tabSalesInvoice
        WHERE customer = :customerName
          AND outstanding_amount > 0
          AND (status IS NULL OR lower(status) NOT IN ('paid','cancelled','canceled'))
          AND docstatus != 2
          AND is_deleted = 0
        ORDER BY posting_date DESC
        """
    )
    suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query(
        """
        SELECT * FROM tabSalesInvoice
        WHERE customer = :customerName
          AND posting_date BETWEEN :startDate AND :endDate
          AND docstatus != 2
          AND is_deleted = 0
        ORDER BY posting_date DESC
        """
    )
    suspend fun getInvoicesForCustomerInRange(
        customerName: String,
        startDate: String,
        endDate: String
    ): List<SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 AND posting_date BETWEEN :startDate AND :endDate ORDER BY posting_date DESC")
    fun getInvoicesByDateRange(
        startDate: String,
        endDate: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Query(
        """
        UPDATE tabSalesInvoice
        SET customer = :newId,
            customer_name = :customerName
        WHERE customer = :oldId
          AND is_deleted = 0
        """
    )
    suspend fun updateCustomerId(oldId: String, newId: String, customerName: String)

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 AND due_date < :today AND outstanding_amount > 0 AND status IN ('Overdue', 'Unpaid', 'Partly Paid') ORDER BY due_date ASC")
    fun getOverdueInvoices(today: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>

    @Transaction
    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 AND posting_date BETWEEN :startDate AND :endDate AND due_date < :today AND outstanding_amount > 0 ORDER BY due_date ASC")
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
        AND is_deleted = 0
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
       UPDATE tabSalesInvoice SET status = :status WHERE invoice_name = :invoiceId AND is_deleted = 0
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
            last_synced_at = :syncedAt,
            remote_payment_entry = :remotePaymentEntry
        WHERE id = :paymentId
        """
    )
    suspend fun updatePaymentSyncStatus(
        paymentId: Int,
        status: String,
        syncedAt: Long,
        remotePaymentEntry: String? = null
    )

    @Query(
        """
        UPDATE tabSalesInvoice
        SET pos_opening_entry = :remoteOpeningEntry
        WHERE pos_opening_entry = :localOpeningEntry
        """
    )
    suspend fun updateInvoicesOpeningEntry(
        localOpeningEntry: String,
        remoteOpeningEntry: String
    )

    @Query(
        """
        UPDATE tabSalesInvoicePayment
        SET pos_opening_entry = :remoteOpeningEntry
        WHERE pos_opening_entry = :localOpeningEntry
        """
    )
    suspend fun updatePaymentsOpeningEntry(
        localOpeningEntry: String,
        remoteOpeningEntry: String
    )

    @Query(
        """
        UPDATE customers
        SET totalPendingAmount = COALESCE(
                (SELECT SUM(outstanding_amount)
                 FROM tabSalesInvoice
                 WHERE customer = :customerId
                   AND outstanding_amount > 0
                   AND docstatus != 2
                   AND is_deleted = 0), 0
            ),
            pendingInvoicesCount = COALESCE(
                (SELECT COUNT(*)
                 FROM tabSalesInvoice
                 WHERE customer = :customerId
                   AND outstanding_amount > 0
                   AND docstatus != 2
                   AND is_deleted = 0), 0
            ),
            currentBalance = COALESCE(
                (SELECT SUM(outstanding_amount)
                 FROM tabSalesInvoice
                 WHERE customer = :customerId
                   AND outstanding_amount > 0
                   AND docstatus != 2
                   AND is_deleted = 0), 0
            ),
            availableCredit = CASE
                WHEN creditLimit IS NULL THEN availableCredit
                ELSE creditLimit - COALESCE(
                    (SELECT SUM(outstanding_amount)
                     FROM tabSalesInvoice
                     WHERE customer = :customerId
                       AND outstanding_amount > 0
                       AND docstatus != 2
                       AND is_deleted = 0), 0
                )
            END,
            state = CASE
                WHEN COALESCE(
                    (SELECT SUM(outstanding_amount)
                     FROM tabSalesInvoice
                     WHERE customer = :customerId
                       AND outstanding_amount > 0
                       AND docstatus != 2
                       AND is_deleted = 0), 0
                ) > 0 THEN 'Pendientes'
                ELSE 'Sin Pendientes'
            END
        WHERE id = :customerId
        """
    )
    suspend fun refreshCustomerSummary(customerId: String)

    @Query("SELECT COUNT(*) FROM tabSalesInvoice WHERE is_deleted = 0")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM tabSalesInvoice WHERE is_deleted = 0 AND sync_status = 'Pending'")
    suspend fun countAllSyncPending(): Int

    @Query(
        """
        SELECT * FROM tabSalesInvoice
        WHERE profile_id = :profileId
          AND created_at BETWEEN :startMillis AND :endMillis
          AND docstatus != 2
          AND is_return = 0
          AND is_deleted = 0
        ORDER BY created_at DESC
        """
    )
    suspend fun getInvoicesForShift(
        profileId: String,
        startMillis: Long,
        endMillis: Long
    ): List<SalesInvoiceEntity>

    @Query(
        """
        SELECT * FROM tabSalesInvoice
        WHERE pos_opening_entry = :openingEntryId
          AND docstatus != 2
          AND is_return = 0
          AND is_deleted = 0
        ORDER BY created_at DESC
        """
    )
    suspend fun getInvoicesForOpeningEntry(openingEntryId: String): List<SalesInvoiceEntity>

    @Query(
        """
        SELECT p.parent_invoice AS invoiceName,
               p.mode_of_payment AS modeOfPayment,
               p.amount AS amount,
               p.entered_amount AS enteredAmount,
               p.payment_currency AS paymentCurrency,
               p.exchange_rate AS exchangeRate,
               i.currency AS invoiceCurrency,
               i.party_account_currency AS partyAccountCurrency
        FROM tabSalesInvoicePayment p
        INNER JOIN tabSalesInvoice i ON i.invoice_name = p.parent_invoice
        WHERE i.profile_id = :profileId
          AND i.created_at BETWEEN :startMillis AND :endMillis
          AND i.docstatus != 2
          AND i.is_return = 0
          AND i.is_deleted = 0
          AND p.sync_status != 'Failed'
        """
    )
    suspend fun getShiftPayments(
        profileId: String,
        startMillis: Long,
        endMillis: Long
    ): List<ShiftPaymentRow>

    @Query(
        """
        SELECT p.parent_invoice AS invoiceName,
               p.mode_of_payment AS modeOfPayment,
               p.amount AS amount,
               p.entered_amount AS enteredAmount,
               p.payment_currency AS paymentCurrency,
               p.exchange_rate AS exchangeRate,
               i.currency AS invoiceCurrency,
               i.party_account_currency AS partyAccountCurrency
        FROM tabSalesInvoicePayment p
        INNER JOIN tabSalesInvoice i ON i.invoice_name = p.parent_invoice
        WHERE i.pos_opening_entry = :openingEntryId
          AND i.docstatus != 2
          AND i.is_return = 0
          AND i.is_deleted = 0
          AND p.sync_status != 'Failed'
        """
    )
    suspend fun getPaymentsForOpeningEntry(openingEntryId: String): List<ShiftPaymentRow>

    @Query(
        """
        SELECT invoice_name
        FROM tabSalesInvoice
        WHERE profile_id = :profileId
          AND outstanding_amount > 0
          AND docstatus != 2
          AND is_return = 0
          AND is_deleted = 0
          AND invoice_name IS NOT NULL
          AND invoice_name NOT LIKE 'LOCAL-%'
        """
    )
    suspend fun getOutstandingInvoiceNamesForProfile(profileId: String): List<String>

    @Query(
        """
        SELECT invoice_name
        FROM tabSalesInvoice
        WHERE outstanding_amount > 0
          AND docstatus != 2
          AND is_return = 0
          AND is_deleted = 0
          AND invoice_name IS NOT NULL
          AND invoice_name NOT LIKE 'LOCAL-%'
        """
    )
    suspend fun getOutstandingInvoiceNames(): List<String>

    @Query("UPDATE tabSalesInvoice SET is_deleted = 1 WHERE is_deleted = 0 AND invoice_name = :invoiceId")
    suspend fun softDeleteByInvoiceId(invoiceId: String)

    @Query("DELETE FROM tabSalesInvoice WHERE is_deleted = 1 AND invoice_name = :invoiceId")
    suspend fun hardDeleteDeletedByInvoiceId(invoiceId: String)

    @Query("DELETE FROM tabSalesInvoice WHERE invoice_name = :invoiceId")
    suspend fun hardDeleteByInvoiceId(invoiceId: String)

    @Query("UPDATE tabSalesInvoice SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM tabSalesInvoice WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("""
        UPDATE tabSalesInvoice
        SET is_deleted = 1
        WHERE is_deleted = 0
          AND profile_id = :profileId
          AND invoice_name NOT IN (:invoiceNames)
    """)
    suspend fun softDeleteNotInForProfile(profileId: String, invoiceNames: List<String>)

    @Query("""
        DELETE FROM tabSalesInvoice
        WHERE is_deleted = 1
          AND profile_id = :profileId
          AND invoice_name NOT IN (:invoiceNames)
    """)
    suspend fun hardDeleteDeletedNotInForProfile(profileId: String, invoiceNames: List<String>)

    @Transaction
    suspend fun deleteInvoiceWithChildren(invoiceId: String) {
        deleteItemsForInvoice(invoiceId)
        deletePaymentsForInvoice(invoiceId)
        hardDeleteByInvoiceId(invoiceId)
    }

    @Transaction
    suspend fun deleteAllWithChildren() {
        deleteAllItems()
        deleteAllPayments()
        hardDeleteAllDeleted()
        softDeleteAll()
    }

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
            discount_amount = :discountAmount,
            grand_total = :grandTotal,
            paid_amount = :paidAmount,
            outstanding_amount = :outstandingAmount,
            base_total = :baseTotal,
            base_net_total = :baseNetTotal,
            base_total_taxes_and_charges = :baseTotalTaxesAndCharges,
            base_grand_total = :baseGrandTotal,
            base_rounding_adjustment = :baseRoundingAdjustment,
            base_rounded_total = :baseRoundedTotal,
            base_discount_amount = :baseDiscountAmount,
            base_paid_amount = :basePaidAmount,
            base_change_amount = :baseChangeAmount,
            base_write_off_amount = :baseWriteOffAmount,
            base_outstanding_amount = :baseOutstandingAmount,
            status = :status,
            docstatus = :docstatus,
            mode_of_payment = :modeOfPayment,
            debit_to = :debitTo,
            remarks = :remarks,
            pos_opening_entry = :posOpeningEntry,
            is_return = :isReturn,
            is_pos = :isPos,
            sync_status = :syncStatus,
            modified_at = :modifiedAt,
            is_deleted = 0
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
        discountAmount: Double,
        grandTotal: Double,
        paidAmount: Double,
        outstandingAmount: Double,
        baseTotal: Double?,
        baseNetTotal: Double?,
        baseTotalTaxesAndCharges: Double?,
        baseGrandTotal: Double?,
        baseRoundingAdjustment: Double?,
        baseRoundedTotal: Double?,
        baseDiscountAmount: Double?,
        basePaidAmount: Double?,
        baseChangeAmount: Double?,
        baseWriteOffAmount: Double?,
        baseOutstandingAmount: Double?,
        status: String,
        docstatus: Int,
        modeOfPayment: String?,
        debitTo: String?,
        remarks: String?,
        posOpeningEntry: String?,
        isReturn: Boolean,
        isPos: Boolean,
        syncStatus: String,
        modifiedAt: Long
    )

    @Query("SELECT * FROM tabSalesInvoice WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldestItem(): SalesInvoiceEntity?

    @Query("DELETE FROM tabSalesInvoiceItem WHERE parent_invoice = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Query("DELETE FROM tabSalesInvoicePayment WHERE parent_invoice = :invoiceId")
    suspend fun deletePaymentsForInvoice(invoiceId: String)

    @Query("DELETE FROM tabSalesInvoiceItem")
    suspend fun deleteAllItems()

    @Query("DELETE FROM tabSalesInvoicePayment")
    suspend fun deleteAllPayments()
}

data class DailySalesTotal(
    val date: String,
    val total: Double
)

data class CurrencySalesSummary(
    val currency: String,
    val total: Double,
    val invoices: Int,
    val customers: Int
)

data class ShiftPaymentRow(
    val invoiceName: String,
    val modeOfPayment: String,
    val amount: Double,
    val enteredAmount: Double,
    val paymentCurrency: String?,
    val exchangeRate: Double,
    val invoiceCurrency: String?,
    val partyAccountCurrency: String?
)

data class CurrencyDailySalesTotal(
    val date: String,
    val currency: String,
    val total: Double
)

data class CurrencyMarginTotal(
    val currency: String,
    val margin: Double
)

data class CurrencyItemCount(
    val currency: String,
    val count: Int
)

data class CurrencyOutstandingTotal(
    val currency: String,
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
