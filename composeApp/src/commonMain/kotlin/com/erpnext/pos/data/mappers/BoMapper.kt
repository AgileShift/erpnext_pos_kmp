package com.erpnext.pos.data.mappers

import androidx.paging.PagingData
import androidx.paging.map
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.domain.models.PendingInvoiceBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.PendingSalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.remoteSource.dto.CategoryDto
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.POSProfileSimpleDto
import com.erpnext.pos.remoteSource.dto.PaymentModesDto
import com.erpnext.pos.remoteSource.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlin.jvm.JvmName

@JvmName("toPagingFlowPendingInvoiceBO")
fun Flow<PagingData<PendingSalesInvoiceEntity>>.toPagingBO(): Flow<PagingData<PendingInvoiceBO>> {
    return transform { value ->
        emit(value.map {
            it.toBO()
        })
    }
}

fun Flow<PagingData<ItemEntity>>.toPagingBO(): Flow<PagingData<ItemBO>> {
    return transform { value ->
        emit(value.map {
            it.toBO()
        })
    }
}

@JvmName("toBOPaymentModesDto")
fun List<PaymentModesDto>.toBO(): List<PaymentModesBO> {
    return this.map { it.toBO() }
}

fun PaymentModesDto.toBO(): PaymentModesBO {
    return PaymentModesBO(
        name = this.name, modeOfPayment = this.modeOfPayment
    )
}

fun UserDto.toBO(): UserBO {
    return UserBO(
        name = this.name,
        username = this.username,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        language = this.language,
        enabled = this.enabled
    )
}

fun CategoryEntity.toBO(): CategoryBO {
    return CategoryBO(
        name = this.name
    )
}

@JvmName("toBOItemDto")
fun List<ItemDto>.toBO(): List<ItemBO> {
    return this.map { it.toBO() }
}

fun ItemDto.toBO(): ItemBO {
    return ItemBO(
        name = this.itemName,
        uom = this.stockUom,
        image = this.image,
        brand = this.brand,
        itemGroup = this.itemGroup,
        itemCode = this.itemCode,
        description = this.description
    )
}

@JvmName("toBOSalesInvoiceEntity")
fun PendingSalesInvoiceEntity.toBO(): PendingInvoiceBO {
    return PendingInvoiceBO(
        invoiceId = this.invoiceId,
        customerId = this.customer,
        customer = this.customerName,
        customerPhone = this.customerPhone,
        postingDate = this.postingDate,
        dueDate = this.dueDate,
        outstandingAmount = this.outstandingAmount,
        netTotal = this.netTotal,
        total = this.grandTotal,
        paidAmount = this.paidAmount,
        isPos = this.isPOS,
        currency = this.currency,
        docStatus = this.docStatus,
        status = this.status,
    )
}

fun ItemEntity.toBO(): ItemBO {
    return ItemBO(
        name = this.name,
        currency = this.currency,
        actualQty = this.actualQty,
        uom = this.stockUom,
        brand = this.brand,
        itemGroup = this.itemGroup,
        itemCode = this.itemCode,
        image = this.image,
        price = this.price,
        discount = this.discount,
        barcode = this.barcode,
        isService = this.isService,
        isStocked = this.isStocked,
        description = this.description,
    )
}

@JvmName("toBOCategoryDto")
fun List<CategoryDto>.toBO(): List<CategoryBO> {
    return this.map { it.toBO() }
}

fun CategoryDto.toBO(): CategoryBO {
    return CategoryBO(
        name = this.name,
    )
}

@JvmName("toProfileDtoToBO")
fun POSProfileDto.toBO(): POSProfileBO {
    return POSProfileBO(
        name = this.profileName,
        warehouse = this.warehouse,
        country = this.country,
        company = this.company,
        currency = this.currency,
        route = this.route,
        incomeAccount = this.incomeAccount,
        expenseAccount = this.expenseAccount,
        paymentModes = this.payments.toBO(),
        branch = this.branch,
        costCenter = this.costCenter,
        applyDiscountOn = this.applyDiscountOn,
        sellingPriceList = this.sellingPriceList
    )
}

fun List<POSProfileSimpleDto>.toBO(): List<POSProfileSimpleBO> {
    return this.map { it.toBO() }
}

fun POSProfileSimpleDto.toBO(): POSProfileSimpleBO {
    return POSProfileSimpleBO(
        name = this.profileName,
        company = this.company,
        currency = this.currency,
        paymentModes = emptyList()
    )
}

fun SalesInvoiceEntity.toBO(): PendingInvoiceBO {
    return PendingInvoiceBO(
        invoiceId = this.invoiceName ?: "",
        status = this.status,
        paidAmount = this.paidAmount,
        customerPhone = "",
        currency = this.currency,
        docStatus = this.docstatus,
        netTotal = this.netTotal,
        customer = this.customer,
        dueDate = this.dueDate,
        outstandingAmount = this.outstandingAmount,
        isPos = false,
        total = this.grandTotal,
        customerId = this.customerName ?: "",
        postingDate = this.postingDate
    )
}