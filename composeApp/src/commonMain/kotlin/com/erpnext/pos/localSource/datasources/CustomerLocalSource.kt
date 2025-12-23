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
}