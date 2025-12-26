package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteDetailItemDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationDetailItemDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationDetailTaxDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderDetailItemDto

fun QuotationDetailItemDto.toEntity(
    quotationId: String,
    instanceId: String,
    companyId: String
) = QuotationItemEntity(
    quotationId = quotationId,
    rowId = rowId,
    itemCode = itemCode,
    itemName = itemName ?: itemCode,
    qty = qty,
    uom = uom,
    rate = rate,
    amount = amount,
    warehouse = warehouse
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}

fun QuotationDetailTaxDto.toEntity(
    quotationId: String,
    instanceId: String,
    companyId: String
) = QuotationTaxEntity(
    quotationId = quotationId,
    chargeType = chargeType,
    accountHead = accountHead,
    rate = rate,
    taxAmount = taxAmount
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}

fun SalesOrderDetailItemDto.toEntity(
    salesOrderId: String,
    instanceId: String,
    companyId: String
) = SalesOrderItemEntity(
    salesOrderId = salesOrderId,
    rowId = rowId,
    itemCode = itemCode,
    itemName = itemName ?: itemCode,
    qty = qty,
    uom = uom,
    rate = rate,
    amount = amount,
    warehouse = warehouse
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}

fun DeliveryNoteDetailItemDto.toEntity(
    deliveryNoteId: String,
    instanceId: String,
    companyId: String
) = DeliveryNoteItemEntity(
    deliveryNoteId = deliveryNoteId,
    rowId = rowId,
    itemCode = itemCode,
    itemName = itemName ?: itemCode,
    qty = qty,
    uom = uom,
    rate = rate,
    amount = amount,
    warehouse = warehouse
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}
