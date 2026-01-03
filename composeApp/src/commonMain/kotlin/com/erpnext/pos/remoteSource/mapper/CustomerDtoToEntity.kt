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

fun POSClosingEntryDto.toEntity(name: String): POSClosingEntryEntity {
    return POSClosingEntryEntity(
        name = name,
        posProfile = this.posProfile,
        posOpeningEntry = this.posOpeningEntry,
        user = this.user,
        periodStartDate = this.periodStartDate,
        periodEndDate = this.periodEndDate,
        closingAmount = 0.0,
        pendingSync = true
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
    address: String,
    contact: ContactChildDto?
): CustomerEntity = CustomerEntity(
    name = name,
    customerName = customerName,
    territory = territory,
    mobileNo = contact?.mobileNo ?: "",
    state = state,
    customerType = customerType,
    creditLimit = creditLimit,
    currentBalance = totalPendingAmount,  // Map totalPendingAmount a currentBalance
    totalPendingAmount = totalPendingAmount,
    pendingInvoicesCount = pendingInvoicesCount,
    availableCredit = availableCredit,
    address = address,
    email = contact?.email ?: ""
)

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
    creditLimit = creditLimit,
    address = address,
    currentBalance = currentBalance,
    pendingInvoices = pendingInvoicesCount,  // Total monto pendiente
    totalPendingAmount = totalPendingAmount,
    availableCredit = availableCredit,
    lastSyncedAt = lastSyncedAt
)