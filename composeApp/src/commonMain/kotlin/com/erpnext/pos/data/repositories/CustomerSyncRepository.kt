package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.CustomerCreatePayload
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.datasources.CustomerOutboxLocalSource
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.CustomerOutboxEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.AddressCreateDto
import com.erpnext.pos.remoteSource.dto.AddressLinkDto
import com.erpnext.pos.remoteSource.dto.AddressUpdateDto
import com.erpnext.pos.remoteSource.dto.ContactCreateDto
import com.erpnext.pos.remoteSource.dto.ContactLinkDto
import com.erpnext.pos.remoteSource.dto.ContactUpdateDto
import com.erpnext.pos.remoteSource.dto.CustomerCreateDto
import com.erpnext.pos.remoteSource.dto.CustomerCreditLimitCreateDto
import com.erpnext.pos.remoteSource.sdk.json
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.views.CashBoxManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CustomerSyncRepository(
    private val api: APIService,
    private val outboxLocalSource: CustomerOutboxLocalSource,
    private val customerLocalSource: CustomerLocalSource,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val cashBoxManager: CashBoxManager
) {
    suspend fun enqueueCustomer(entity: CustomerEntity, payload: CustomerCreatePayload) {
        customerLocalSource.insert(entity)
        val outbox = CustomerOutboxEntity(
            localId = entity.name,
            customerLocalId = entity.name,
            payloadJson = json.encodeToString(payload),
            status = "Pending",
            attempts = 0,
            lastError = null,
            remoteId = null,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            lastAttemptAt = null
        )
        outboxLocalSource.insert(outbox)
    }

    suspend fun pushPending(): Boolean {
        val pending = outboxLocalSource.getPending()
        if (pending.isEmpty()) return false
        var hasChanges = false
        val attemptAt = Clock.System.now().toEpochMilliseconds()
        pending.forEach { item ->
            try {
                val payload = json.decodeFromString<CustomerCreatePayload>(item.payloadJson)
                val remoteId = item.remoteId ?: run {
                    val creditLimits = payload.creditLimit?.let {
                        listOf(
                            CustomerCreditLimitCreateDto(
                                company = payload.representsCompany
                                    ?: cashBoxManager.requireContext().company,
                                creditLimit = it
                            )
                        )
                    }
                    val response = api.createCustomer(
                        CustomerCreateDto(
                            customerName = payload.customerName,
                            customerType = payload.customerType,
                            customerGroup = payload.customerGroup,
                            territory = payload.territory,
                            defaultCurrency = payload.defaultCurrency,
                            defaultPriceList = payload.defaultPriceList,
                            mobileNo = payload.mobileNo ?: payload.phone,
                            emailId = payload.email,
                            taxId = payload.taxId,
                            taxCategory = payload.taxCategory,
                            isInternalCustomer = payload.isInternalCustomer,
                            representsCompany = payload.representsCompany,
                            creditLimits = creditLimits,
                            paymentTerms = payload.paymentTerms,
                            customerDetails = payload.notes
                        )
                    )
                    outboxLocalSource.updateRemoteId(item.localId, response.name)
                    customerLocalSource.updateCustomerId(
                        oldId = item.customerLocalId,
                        newId = response.name,
                        customerName = payload.customerName
                    )
                    salesInvoiceDao.updateCustomerId(
                        oldId = item.customerLocalId,
                        newId = response.name,
                        customerName = payload.customerName
                    )
                    response.name
                }

                payload.contact?.let { contact ->
                    if (!contact.email.isNullOrBlank() || !contact.mobile.isNullOrBlank() ||
                        !contact.phone.isNullOrBlank()
                    ) {
                        val existing = runCatching { api.findCustomerContacts(remoteId) }
                            .getOrElse { emptyList() }
                        if (existing.isNotEmpty()) {
                            api.updateContact(
                                existing.first().name,
                                ContactUpdateDto(
                                    emailId = contact.email,
                                    mobileNo = contact.mobile,
                                    phone = contact.phone
                                )
                            )
                        } else {
                            api.createContact(
                                ContactCreateDto(
                                    firstName = payload.customerName,
                                    emailId = contact.email,
                                    mobileNo = contact.mobile,
                                    phone = contact.phone,
                                    links = listOf(
                                        ContactLinkDto(
                                            linkDoctype = "Customer",
                                            linkName = remoteId
                                        )
                                    )
                                )
                            )
                        }
                    }
                }

                payload.address?.let { address ->
                    if (!address.line1.isNullOrBlank() || !address.city.isNullOrBlank()) {
                        val existing = runCatching { api.findCustomerAddresses(remoteId) }
                            .getOrElse { emptyList() }
                        if (existing.isNotEmpty()) {
                            api.updateAddress(
                                existing.first().name,
                                AddressUpdateDto(
                                    addressTitle = payload.customerName,
                                    addressType = "Billing",
                                    addressLine1 = address.line1,
                                    addressLine2 = address.line2,
                                    city = address.city,
                                    state = address.state,
                                    country = address.country,
                                    emailId = payload.email,
                                    phone = payload.mobileNo ?: payload.phone
                                )
                            )
                        } else {
                            api.createAddress(
                                AddressCreateDto(
                                    addressTitle = payload.customerName,
                                    addressType = "Billing",
                                    addressLine1 = address.line1,
                                    addressLine2 = address.line2,
                                    city = address.city,
                                    state = address.state,
                                    country = address.country,
                                    emailId = payload.email,
                                    phone = payload.mobileNo ?: payload.phone,
                                    links = listOf(
                                        AddressLinkDto(
                                            linkDoctype = "Customer",
                                            linkName = remoteId
                                        )
                                    )
                                )
                            )
                        }
                    }
                }

                outboxLocalSource.updateStatus(
                    localId = item.localId,
                    status = "Synced",
                    error = null,
                    attemptIncrement = 1,
                    attemptAt = attemptAt
                )
                hasChanges = true
            } catch (e: Exception) {
                AppSentry.capture(e, "CustomerSyncRepository.pushPending failed")
                AppLogger.warn("CustomerSyncRepository.pushPending failed", e)
                outboxLocalSource.updateStatus(
                    localId = item.localId,
                    status = "Failed",
                    error = e.message,
                    attemptIncrement = 1,
                    attemptAt = attemptAt
                )
            }
        }
        return hasChanges
    }
}
