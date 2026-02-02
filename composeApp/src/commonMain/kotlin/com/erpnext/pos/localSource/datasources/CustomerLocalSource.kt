package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import kotlinx.coroutines.flow.Flow

interface ICustomerLocalSource {
    suspend fun insert(customer: CustomerEntity)
    suspend fun insertAll(customers: List<CustomerEntity>)
    suspend fun getAll(): Flow<List<CustomerEntity>>
    fun getAllFiltered(search: String): Flow<List<CustomerEntity>>
    fun getByTerritory(territory: String, search: String?): Flow<List<CustomerEntity>>
    suspend fun getByName(name: String): CustomerEntity?
    suspend fun count(): Int
    suspend fun getByCustomerState(state: String): Flow<List<CustomerEntity>>
    suspend fun getOldestCustomer(): CustomerEntity?
    suspend fun saveInvoices(invoices: List<SalesInvoiceWithItemsAndPayments>)
    suspend fun refreshCustomerSummary(customerId: String)
    suspend fun getOutstandingInvoicesForCustomer(customerName: String): List<SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoicesForCustomerInRange(
        customerName: String,
        startDate: String,
        endDate: String
    ): List<SalesInvoiceWithItemsAndPayments>
    suspend fun getOutstandingInvoiceNamesForProfile(profileId: String): List<String>
    suspend fun getOutstandingInvoiceNames(): List<String>
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?
    suspend fun getInvoiceNamesMissingItems(profileId: String, limit: Int = 50): List<String>
    suspend fun deleteInvoiceById(invoiceName: String)
    suspend fun updateSummary(
        customerId: String,
        totalPendingAmount: Double,
        pendingInvoicesCount: Int,
        currentBalance: Double,
        availableCredit: Double?,
        state: String
    )
    suspend fun updateCustomerId(oldId: String, newId: String, customerName: String)
    suspend fun deleteMissing(customerIds: List<String>)
}

class CustomerLocalSource(private val dao: CustomerDao, private val invoiceDao: SalesInvoiceDao) :
    ICustomerLocalSource {
    override suspend fun insert(customer: CustomerEntity) = dao.insert(customer)
    override suspend fun insertAll(customers: List<CustomerEntity>) = dao.insertAll(customers)

    override suspend fun getAll(): Flow<List<CustomerEntity>> = dao.getAll()

    override suspend fun saveInvoices(invoices: List<SalesInvoiceWithItemsAndPayments>) {
        invoiceDao.insertFullInvoices(invoices)
    }

    override fun getAllFiltered(search: String): Flow<List<CustomerEntity>> =
        dao.getAllFiltered(search)

    override fun getByTerritory(territory: String, search: String?): Flow<List<CustomerEntity>> =
        dao.getByTerritory(territory, search)

    override suspend fun getByName(name: String): CustomerEntity? = dao.getByName(name)

    override suspend fun count(): Int {
        return dao.count()
    }

    override suspend fun getOldestCustomer(): CustomerEntity? {
        return dao.getOldestCustomer()
    }

    override suspend fun getByCustomerState(state: String): Flow<List<CustomerEntity>> {
        return dao.getByCustomerState(state)
    }

    override suspend fun refreshCustomerSummary(customerId: String) {
        invoiceDao.refreshCustomerSummary(customerId)
    }

    override suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return invoiceDao.getOutstandingInvoicesForCustomer(customerName)
    }

    override suspend fun getInvoicesForCustomerInRange(
        customerName: String,
        startDate: String,
        endDate: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return invoiceDao.getInvoicesForCustomerInRange(
            customerName = customerName,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getOutstandingInvoiceNamesForProfile(
        profileId: String
    ): List<String> {
        return invoiceDao.getOutstandingInvoiceNamesForProfile(profileId)
    }

    override suspend fun getOutstandingInvoiceNames(): List<String> {
        return invoiceDao.getOutstandingInvoiceNames()
    }

    override suspend fun getInvoiceByName(
        invoiceName: String
    ): SalesInvoiceWithItemsAndPayments? {
        return invoiceDao.getInvoiceByName(invoiceName)
    }

    override suspend fun getInvoiceNamesMissingItems(
        profileId: String,
        limit: Int
    ): List<String> {
        return invoiceDao.getInvoiceNamesMissingItems(profileId, limit)
    }

    override suspend fun deleteInvoiceById(invoiceName: String) {
        invoiceDao.hardDeleteDeletedByInvoiceId(invoiceName)
        invoiceDao.softDeleteByInvoiceId(invoiceName)
    }

    override suspend fun updateSummary(
        customerId: String,
        totalPendingAmount: Double,
        pendingInvoicesCount: Int,
        currentBalance: Double,
        availableCredit: Double?,
        state: String
    ) {
        dao.updateSummary(
            customerId = customerId,
            totalPendingAmount = totalPendingAmount,
            pendingInvoicesCount = pendingInvoicesCount,
            currentBalance = currentBalance,
            availableCredit = availableCredit,
            state = state
        )
    }

    override suspend fun updateCustomerId(oldId: String, newId: String, customerName: String) {
        dao.updateCustomerId(oldId = oldId, newId = newId, customerName = customerName)
    }

    override suspend fun deleteMissing(customerIds: List<String>) {
        val ids = customerIds.ifEmpty { listOf("__empty__") }
        dao.hardDeleteDeletedNotIn(ids)
        dao.softDeleteNotIn(ids)
    }
}
