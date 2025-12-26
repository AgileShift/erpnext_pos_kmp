package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ICustomerRepository
import com.erpnext.pos.domain.sync.PendingSync
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.CustomerDao
import com.erpnext.pos.localSource.dao.v2.DeliveryNoteDao
import com.erpnext.pos.localSource.dao.v2.PaymentEntryDao
import com.erpnext.pos.localSource.dao.v2.QuotationDao
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.v2.SalesOrderDao
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.erpnext.pos.remoteSource.dto.v2.CustomerCreateDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerDto
import com.erpnext.pos.remoteSource.mapper.v2.CustomerMappedEntities
import com.erpnext.pos.remoteSource.mapper.v2.toEntities
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.IncrementalSyncFilters

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val salesOrderDao: SalesOrderDao,
    private val quotationDao: QuotationDao,
    private val deliveryNoteDao: DeliveryNoteDao,
    private val paymentEntryDao: PaymentEntryDao,
    private val api: APIServiceV2
) : ICustomerRepository {

    suspend fun pull(ctx: SyncContext): Boolean {
        val customers = api.list<CustomerDto>(
            doctype = ERPDocType.Customer,
            filters = IncrementalSyncFilters.customer(ctx)
        )
        if (customers.isEmpty()) return false

        val mapped = customers.map { it.toEntities(ctx.instanceId, ctx.companyId) }
        val customerEntities = mapped.map(CustomerMappedEntities::customer)
        val contactEntities = mapped.flatMap(CustomerMappedEntities::contacts)
        val addressEntities = mapped.flatMap(CustomerMappedEntities::addresses)

        customerDao.upsertCustomers(customerEntities)
        if (contactEntities.isNotEmpty()) {
            customerDao.upsertContacts(contactEntities)
        }
        if (addressEntities.isNotEmpty()) {
            customerDao.upsertAddresses(addressEntities)
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    suspend fun pushPending(ctx: SyncContext): List<PendingSync<CustomerCreateDto>> {
        resolvePendingDuplicates(ctx.instanceId, ctx.companyId)
        return buildPendingCreatePayloads(ctx.instanceId, ctx.companyId)
    }

    override suspend fun getCustomersForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ) = customerDao.getCustomersWithContactsAndAddressesForTerritory(
        instanceId,
        companyId,
        territoryId
    )

    override suspend fun getCustomerDetail(
        instanceId: String,
        companyId: String,
        customerId: String
    ) = customerDao.getCustomerWithContactsAndAddresses(
        instanceId,
        companyId,
        customerId
    )

    suspend fun insertCustomerWithDetails(
        customer: CustomerEntity,
        contacts: List<CustomerContactEntity>,
        addresses: List<CustomerAddressEntity>
    ) {
        customerDao.insertCustomerWithDetails(customer, contacts, addresses)
    }

    suspend fun getPendingCustomersWithDetails(
        instanceId: String,
        companyId: String
    ) = customerDao.getPendingCustomersWithContactsAndAddresses(instanceId, companyId)

    suspend fun resolveRemoteCustomerId(
        instanceId: String,
        companyId: String,
        customerId: String
    ): String {
        return customerDao.getRemoteCustomerId(instanceId, companyId, customerId) ?: customerId
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markCustomerSynced(
        instanceId: String,
        companyId: String,
        customerId: String,
        remoteName: String,
        remoteModified: String?
    ) {
        val now = Clock.System.now().epochSeconds
        customerDao.markCustomerSynced(
            instanceId = instanceId,
            companyId = companyId,
            customerId = customerId,
            remoteName = remoteName,
            remoteModified = remoteModified,
            lastSyncedAt = now,
            updatedAt = now
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markFailed(
        instanceId: String,
        companyId: String,
        customerId: String
    ) {
        val now = Clock.System.now().epochSeconds
        customerDao.updateSyncStatus(
            instanceId = instanceId,
            companyId = companyId,
            customerId = customerId,
            syncStatus = "FAILED",
            lastSyncedAt = null,
            updatedAt = now
        )
    }

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<PendingSync<CustomerCreateDto>> {
        return customerDao
            .getPendingCustomersWithContactsAndAddresses(instanceId, companyId)
            .map { snapshot ->
                PendingSync(
                    localId = snapshot.customer.customerId,
                    payload = CustomerCreateDto(
                        customerName = snapshot.customer.customerName,
                        customerType = snapshot.customer.customerType,
                        territory = snapshot.customer.territory,
                        customerGroup = snapshot.customer.customerGroup,
                        defaultCurrency = snapshot.customer.defaultCurrency,
                        defaultPriceList = snapshot.customer.defaultPriceList,
                        mobileNo = snapshot.customer.mobileNo
                    )
                )
            }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun resolvePendingDuplicates(instanceId: String, companyId: String) {
        val pending = customerDao.getPendingCustomersWithContactsAndAddresses(instanceId, companyId)
        if (pending.isEmpty()) return

        val now = Clock.System.now().epochSeconds
        pending.forEach { snapshot ->
            val match = customerDao.findSyncedCustomerMatch(
                instanceId = instanceId,
                companyId = companyId,
                customerName = snapshot.customer.customerName,
                mobileNo = snapshot.customer.mobileNo
            ) ?: return@forEach

            salesInvoiceDao.replaceCustomerReference(
                instanceId = instanceId,
                companyId = companyId,
                oldCustomerId = snapshot.customer.customerId,
                newCustomerId = match.customerId,
                newCustomerName = match.customerName,
                updatedAt = now
            )
            salesOrderDao.replaceCustomerReference(
                instanceId = instanceId,
                companyId = companyId,
                oldCustomerId = snapshot.customer.customerId,
                newCustomerId = match.customerId,
                newCustomerName = match.customerName,
                updatedAt = now
            )
            quotationDao.replaceCustomerReference(
                instanceId = instanceId,
                companyId = companyId,
                oldCustomerId = snapshot.customer.customerId,
                newCustomerId = match.customerId,
                newCustomerName = match.customerName,
                updatedAt = now
            )
            quotationDao.replaceCustomerLinkReference(
                instanceId = instanceId,
                companyId = companyId,
                oldCustomerId = snapshot.customer.customerId,
                newCustomerId = match.customerId,
                newCustomerName = match.customerName,
                updatedAt = now
            )
            deliveryNoteDao.replaceCustomerReference(
                instanceId = instanceId,
                companyId = companyId,
                oldCustomerId = snapshot.customer.customerId,
                newCustomerId = match.customerId,
                newCustomerName = match.customerName,
                updatedAt = now
            )
            paymentEntryDao.replaceCustomerReference(
                instanceId = instanceId,
                companyId = companyId,
                oldCustomerId = snapshot.customer.customerId,
                newCustomerId = match.customerId,
                updatedAt = now
            )

            customerDao.markMergedCustomer(
                instanceId = instanceId,
                companyId = companyId,
                customerId = snapshot.customer.customerId,
                remoteName = match.customerId,
                remoteModified = match.remoteModified,
                lastSyncedAt = now,
                updatedAt = now,
                isDeleted = true
            )
        }
    }
}
