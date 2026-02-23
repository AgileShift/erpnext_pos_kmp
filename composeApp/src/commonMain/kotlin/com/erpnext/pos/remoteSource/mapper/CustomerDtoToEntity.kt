package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.remoteSource.dto.BalanceDetailsDto
import com.erpnext.pos.remoteSource.dto.ContactChildDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.CustomerReceivableAccountDto
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto

fun POSOpeningEntryDto.toEntity(name: String): POSOpeningEntryEntity {
    return POSOpeningEntryEntity(
        name = name,
        posProfile = this.posProfile,
        company = this.company,
        periodStartDate = this.periodStartDate,
        postingDate = this.postingDate,
        user = this.user,
        pendingSync = true
    )
}

fun POSClosingEntryDto.toEntity(name: String, pendingSync: Boolean = true): POSClosingEntryEntity {
    val totalClosing = paymentReconciliation.sumOf { it.closingAmount }
    return POSClosingEntryEntity(
        name = name,
        posProfile = this.posProfile,
        posOpeningEntry = this.posOpeningEntry,
        user = this.user,
        periodStartDate = this.periodStartDate,
        periodEndDate = this.periodEndDate,
        closingAmount = totalClosing,
        pendingSync = pendingSync
    )
}

fun List<CustomerBO>.toEntity(): List<CustomerEntity> {
    return this.map { it.toEntity() }
}

fun CustomerBO.toEntity(): CustomerEntity {
    return CustomerEntity(
        name = this.name,
        customerName = this.customerName,
        territory = this.territory,
        email = this.email,
        mobileNo = this.mobileNo,
        customerType = this.customerType,
        creditLimit = this.creditLimit,
        currentBalance = this.currentBalance,
        totalPendingAmount = this.totalPendingAmount,
        pendingInvoicesCount = this.pendingInvoices,
        availableCredit = this.availableCredit,
        address = this.address,
    )
}

fun CustomerDto.toEntity(
    creditLimit: Double? = null,
    availableCredit: Double?,
    pendingInvoicesCount: Int,
    totalPendingAmount: Double,
    state: String? = null,
    receivableAccount: String? = null,
    receivableAccountCurrency: String? = null,
): CustomerEntity = CustomerEntity(
    name = name,
    customerName = customerName,
    territory = territory,
    mobileNo = mobileNo,
    state = state,
    image = image,
    customerType = customerType,
    creditLimit = creditLimit,
    currentBalance = totalPendingAmount,  // Map totalPendingAmount a currentBalance
    totalPendingAmount = totalPendingAmount,
    pendingInvoicesCount = pendingInvoicesCount,
    availableCredit = availableCredit,
    partyAccountCurrency = partyAccountCurrency,
    receivableAccount = receivableAccount,
    receivableAccountCurrency = receivableAccountCurrency,
    address = address,
    email = email
)

fun CustomerDto.resolveReceivableAccount(company: String?): CustomerReceivableAccountDto? {
    val normalizedCompany = company?.trim()?.lowercase()
    if (!normalizedCompany.isNullOrBlank()) {
        receivableAccounts.firstOrNull { account ->
            account.company?.trim()?.lowercase() == normalizedCompany
        }?.let { return it }
    }
    return receivableAccounts.firstOrNull()
}

fun List<BalanceDetailsDto>.toEntity(cashboxId: Long): List<BalanceDetailsEntity> {
    return this.map { it.toEntity(cashboxId) }
}

fun BalanceDetailsDto.toEntity(cashboxId: Long): BalanceDetailsEntity {
    return BalanceDetailsEntity(
        cashboxId = cashboxId,
        modeOfPayment = this.modeOfPayment,
        openingAmount = this.openingAmount
    )
}

fun CustomerEntity.toBO(): CustomerBO = CustomerBO(
    name = name,
    customerName = customerName,
    territory = territory,
    mobileNo = mobileNo,
    customerType = customerType,
    state = state,
    image = image,
    creditLimit = creditLimit,
    address = address,
    currentBalance = currentBalance,
    pendingInvoices = pendingInvoicesCount,  // Total monto pendiente
    totalPendingAmount = totalPendingAmount,
    availableCredit = availableCredit,
    partyAccountCurrency = partyAccountCurrency,
    receivableAccount = receivableAccount,
    receivableAccountCurrency = receivableAccountCurrency,
    lastSyncedAt = lastSyncedAt
)
