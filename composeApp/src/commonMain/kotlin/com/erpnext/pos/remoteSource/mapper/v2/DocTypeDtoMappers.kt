package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.localSource.entities.v2.PricingRuleEntity
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.remoteSource.dto.v2.CustomerAddressDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerContactDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerSnapshot
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteHeaderDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteItemDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteLinkDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteSnapshot
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntrySnapshot
import com.erpnext.pos.remoteSource.dto.v2.PricingRuleDto
import com.erpnext.pos.remoteSource.dto.v2.PricingRuleSnapshot
import com.erpnext.pos.remoteSource.dto.v2.QuotationCustomerLinkDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationHeaderDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationItemDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationSnapshot
import com.erpnext.pos.remoteSource.dto.v2.QuotationTaxDto
import com.erpnext.pos.remoteSource.dto.v2.SalesInvoiceSnapshot
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderHeaderDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderItemDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderSnapshot

data class CustomerMappedEntities(
    val customer: CustomerEntity,
    val contacts: List<CustomerContactEntity>,
    val addresses: List<CustomerAddressEntity>
)

fun CustomerDto.toEntities(instanceId: String, companyId: String): CustomerMappedEntities {
    val customer = CustomerEntity(
        customerId = customerId,
        customerName = customerName,
        territoryId = territory,
        customerType = customerType,
        disabled = disabled,
        customerGroup = customerGroup,
        territory = territory,
        creditLimit = null,
        paymentTerms = null,
        defaultCurrency = defaultCurrency,
        defaultPriceList = defaultPriceList,
        primaryAddress = primaryAddressId,
        mobileNo = mobileNo,
        outstandingAmount = null,
        overdueAmount = null,
        unpaidInvoiceCount = null,
        lastPaidDate = null,
        syncStatus = null
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

    val contacts = primaryContact?.let { listOf(it.toEntity(customerId, instanceId, companyId)) } ?: emptyList()
    val addresses = primaryAddress?.let { listOf(it.toEntity(customerId, instanceId, companyId)) } ?: emptyList()
    return CustomerMappedEntities(customer, contacts, addresses)
}

fun CustomerSnapshot.toEntities(instanceId: String, companyId: String): CustomerMappedEntities {
    val customer = CustomerEntity(
        customerId = customerId,
        customerName = customerName,
        territoryId = territoryId,
        customerType = "Individual",
        disabled = false,
        customerGroup = null,
        territory = territoryId,
        creditLimit = creditLimit,
        paymentTerms = null,
        defaultCurrency = null,
        defaultPriceList = null,
        primaryAddress = primaryAddress?.addressId,
        mobileNo = mobileFallback ?: primaryPhone,
        outstandingAmount = outstandingAmount,
        overdueAmount = overdueAmount,
        unpaidInvoiceCount = null,
        lastPaidDate = lastPurchaseDate,
        syncStatus = null
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

    val contacts = primaryContact?.let { listOf(it.toEntity(customerId, instanceId, companyId)) } ?: emptyList()
    val addresses = primaryAddress?.let { listOf(it.toEntity(customerId, instanceId, companyId)) } ?: emptyList()
    return CustomerMappedEntities(customer, contacts, addresses)
}

fun CustomerContactDto.toEntity(
    customerId: String,
    instanceId: String,
    companyId: String
) = CustomerContactEntity(
    contactId = name ?: "${customerId}-contact",
    customerId = customerId,
    firstName = name?.split(" ")?.firstOrNull().orEmpty(),
    lastName = name?.split(" ")?.drop(1)?.joinToString(" ")?.takeIf { it.isNotBlank() },
    fullName = name ?: customerId,
    emailId = email,
    phone = phone,
    mobileNo = mobile,
    isPrimary = true,
    status = "Active"
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}

fun CustomerAddressDto.toEntity(
    customerId: String,
    instanceId: String,
    companyId: String
) = CustomerAddressEntity(
    addressId = addressId,
    customerId = customerId,
    addressTitle = title,
    addressType = type,
    line1 = line1,
    line2 = line2,
    city = city,
    county = null,
    state = null,
    country = country,
    pinCode = null,
    isPrimary = true,
    isShipping = false,
    disabled = false
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}

fun QuotationHeaderDto.toEntity(instanceId: String, companyId: String) =
    QuotationEntity(
        quotationId = quotationId,
        transactionDate = transactionDate,
        validUntil = validUntil,
        company = company,
        partyName = partyName,
        customerName = customerName,
        territory = territory,
        status = status,
        priceListCurrency = priceListCurrency,
        sellingPriceList = sellingPriceList,
        netTotal = netTotal,
        grandTotal = grandTotal
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun QuotationItemDto.toEntity(instanceId: String, companyId: String) =
    QuotationItemEntity(
        quotationId = quotationId,
        rowId = rowId,
        itemCode = itemCode,
        itemName = itemName,
        qty = qty,
        uom = uom,
        rate = rate,
        amount = amount,
        warehouse = warehouse
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun QuotationTaxDto.toEntity(instanceId: String, companyId: String) =
    QuotationTaxEntity(
        quotationId = quotationId,
        chargeType = chargeType,
        accountHead = accountHead,
        rate = rate,
        taxAmount = taxAmount
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun QuotationCustomerLinkDto.toEntity(instanceId: String, companyId: String) =
    QuotationCustomerLinkEntity(
        quotationId = quotationId,
        partyName = partyName,
        customerName = customerName,
        contactId = contactId,
        addressId = addressId
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun QuotationSnapshot.toEntity(instanceId: String, companyId: String) =
    QuotationEntity(
        quotationId = quotationId,
        transactionDate = transactionDate,
        validUntil = validUntil,
        company = company,
        partyName = partyName,
        customerName = customerName,
        territory = territory,
        status = status,
        priceListCurrency = priceListCurrency,
        sellingPriceList = sellingPriceList,
        netTotal = netTotal,
        grandTotal = grandTotal
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun SalesOrderHeaderDto.toEntity(instanceId: String, companyId: String) =
    SalesOrderEntity(
        salesOrderId = salesOrderId,
        transactionDate = transactionDate,
        deliveryDate = deliveryDate,
        company = company,
        customerId = customerId,
        customerName = customerName,
        territory = territory,
        status = status,
        deliveryStatus = deliveryStatus,
        billingStatus = billingStatus,
        priceListCurrency = priceListCurrency,
        sellingPriceList = sellingPriceList,
        netTotal = netTotal,
        grandTotal = grandTotal
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun SalesOrderSnapshot.toEntity(instanceId: String, companyId: String) =
    SalesOrderEntity(
        salesOrderId = salesOrderId,
        transactionDate = transactionDate,
        deliveryDate = deliveryDate,
        company = company,
        customerId = customerId,
        customerName = customerName,
        territory = territory,
        status = status,
        deliveryStatus = deliveryStatus,
        billingStatus = billingStatus,
        priceListCurrency = priceListCurrency,
        sellingPriceList = sellingPriceList,
        netTotal = netTotal,
        grandTotal = grandTotal
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun SalesOrderItemDto.toEntity(instanceId: String, companyId: String) =
    SalesOrderItemEntity(
        salesOrderId = salesOrderId,
        rowId = rowId,
        itemCode = itemCode,
        itemName = itemName,
        qty = qty,
        uom = uom,
        rate = rate,
        amount = amount,
        warehouse = warehouse
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun PaymentEntryDto.toEntity(instanceId: String, companyId: String) =
    PaymentEntryEntity(
        paymentEntryId = paymentEntryId,
        postingDate = postingDate,
        company = company,
        territory = territory,
        paymentType = paymentType,
        modeOfPayment = modeOfPayment,
        partyType = partyType,
        partyId = partyId,
        paidAmount = paidAmount,
        receivedAmount = receivedAmount,
        unallocatedAmount = unallocatedAmount
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun PaymentEntrySnapshot.toEntity(instanceId: String, companyId: String) =
    PaymentEntryEntity(
        paymentEntryId = paymentEntryId,
        postingDate = postingDate,
        company = company,
        territory = territory,
        paymentType = paymentType,
        modeOfPayment = modeOfPayment,
        partyType = partyType,
        partyId = partyId,
        paidAmount = paidAmount,
        receivedAmount = receivedAmount,
        unallocatedAmount = unallocatedAmount
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun PaymentEntryReferenceDto.toEntity(instanceId: String, companyId: String) =
    PaymentEntryReferenceEntity(
        paymentEntryId = paymentEntryId,
        referenceDoctype = referenceDoctype,
        referenceName = referenceName,
        totalAmount = totalAmount,
        outstandingAmount = outstandingAmount,
        allocatedAmount = allocatedAmount
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun DeliveryNoteHeaderDto.toEntity(instanceId: String, companyId: String) =
    DeliveryNoteEntity(
        deliveryNoteId = deliveryNoteId,
        postingDate = postingDate,
        company = company,
        customerId = customerId,
        customerName = customerName,
        territory = territory,
        status = status,
        setWarehouse = setWarehouse
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun DeliveryNoteSnapshot.toEntity(instanceId: String, companyId: String) =
    DeliveryNoteEntity(
        deliveryNoteId = deliveryNoteId,
        postingDate = postingDate,
        company = company,
        customerId = customerId,
        customerName = customerName,
        territory = territory,
        status = status,
        setWarehouse = setWarehouse
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun DeliveryNoteItemDto.toEntity(instanceId: String, companyId: String) =
    DeliveryNoteItemEntity(
        deliveryNoteId = deliveryNoteId,
        rowId = rowId,
        itemCode = itemCode,
        itemName = itemName,
        qty = qty,
        uom = uom,
        rate = rate,
        amount = amount,
        warehouse = warehouse
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun DeliveryNoteLinkDto.toEntity(instanceId: String, companyId: String) =
    DeliveryNoteLinkEntity(
        deliveryNoteId = deliveryNoteId,
        salesOrderId = salesOrderId,
        salesInvoiceId = salesInvoiceId
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun PricingRuleDto.toEntity(instanceId: String, companyId: String) =
    PricingRuleEntity(
        pricingRuleId = pricingRuleId,
        priority = priority,
        condition = condition,
        territory = territory,
        forPriceList = forPriceList,
        otherItemCode = otherItemCode,
        otherItemGroup = otherItemGroup,
        validFrom = validFrom,
        validUntil = validUntil
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun PricingRuleSnapshot.toEntity(instanceId: String, companyId: String) =
    PricingRuleEntity(
        pricingRuleId = pricingRuleId,
        priority = priority,
        condition = condition,
        territory = territory,
        forPriceList = forPriceList,
        otherItemCode = otherItemCode,
        otherItemGroup = otherItemGroup,
        validFrom = validFrom,
        validUntil = validUntil
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun SalesInvoiceSnapshot.toEntity(instanceId: String, companyId: String) =
    SalesInvoiceEntity(
        invoiceId = invoiceId,
        territoryId = territory ?: "",
        namingSeries = "REMOTE",
        docStatus = docStatus.toDocStatus(),
        status = status ?: "Draft",
        postingDate = postingDate,
        postingTime = "00:00:00",
        customerId = customerId,
        customerName = customerName ?: contactDisplay ?: customerId,
        company = company,
        territory = territory ?: "",
        salesPerson = "",
        currency = currency ?: "NIO",
        conversionRate = 1f,
        total = netTotal,
        totalTaxesAndCharges = (grandTotal - netTotal).toFloat(),
        grandTotal = grandTotal,
        roundedTotal = null,
        outstandingAmount = outstandingAmount ?: 0.0,
        isPos = isPos,
        dueDate = dueDate ?: postingDate,
        paymentTerms = null,
        updateStock = true,
        setWarehouse = null,
        priceList = null,
        disableRoundedTotal = false,
        syncStatus = null,
        remoteModified = modified,
        remoteName = invoiceId
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

private fun Int?.toDocStatus(): String =
    when (this) {
        1 -> "Submitted"
        2 -> "Cancelled"
        0 -> "Draft"
        else -> "Draft"
    }
