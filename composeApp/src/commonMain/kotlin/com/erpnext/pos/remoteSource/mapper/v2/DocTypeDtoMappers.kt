package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.localSource.entities.v2.PricingRuleEntity
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
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
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderHeaderDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderItemDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderSnapshot

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
