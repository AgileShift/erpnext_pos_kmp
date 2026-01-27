package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.AddressEntity
import com.erpnext.pos.localSource.entities.ContactEntity
import com.erpnext.pos.remoteSource.dto.AddressListDto
import com.erpnext.pos.remoteSource.dto.ContactListDto
import kotlin.time.Clock

private fun findCustomerLink(links: List<com.erpnext.pos.remoteSource.dto.LinkRefDto>): String? {
    return links.firstOrNull { it.linkDoctype.equals("Customer", ignoreCase = true) }?.linkName
}

fun ContactListDto.toEntity(): ContactEntity {
    val customerId = findCustomerLink(links)
    return ContactEntity(
        name = name,
        customerId = customerId,
        emailId = emailId,
        mobileNo = mobileNo,
        phone = phone,
        isDeleted = false,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

fun AddressListDto.toEntity(): AddressEntity {
    val customerId = findCustomerLink(links)
    return AddressEntity(
        name = name,
        customerId = customerId,
        addressTitle = addressTitle,
        addressType = addressType,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        city = city,
        state = state,
        country = country,
        emailId = emailId,
        phone = phone,
        isDeleted = false,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}
