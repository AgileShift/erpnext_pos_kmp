package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import kotlinx.coroutines.flow.Flow

interface ICustomerLocalSource {
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
    suspend fun getOutstandingInvoiceNamesForProfile(profileId: String): List<String>
    suspend fun getOutstandingInvoiceNames(): List<String>
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?
    suspend fun deleteInvoiceById(invoiceName: String)
    suspend fun updateSummary(
        customerId: String,
        totalPendingAmount: Double,
        pendingInvoicesCount: Int,
        currentBalance: Double,
        availableCredit: Double?,
        state: String
    )
    suspend fun deleteMissing(customerIds: List<String>)
}

class CustomerLocalSource(private val dao: CustomerDao, private val invoiceDao: SalesInvoiceDao) :
    ICustomerLocalSource {
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

    override suspend fun deleteInvoiceById(invoiceName: String) {
        invoiceDao.deleteByInvoiceId(invoiceName)
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

    override suspend fun deleteMissing(customerIds: List<String>) {
        if (customerIds.isEmpty()) {
            dao.deleteAll()
        } else {
            dao.deleteNotIn(customerIds)
        }
    }
}
