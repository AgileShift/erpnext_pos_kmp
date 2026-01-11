package com.erpnext.pos.data.mappers

import androidx.paging.PagingData
import androidx.paging.map
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.models.SalesInvoiceItemsBO
import com.erpnext.pos.domain.models.SalesInvoicePaymentsBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.localSource.entities.PaymentTermEntity
import com.erpnext.pos.localSource.entities.DeliveryChargeEntity
import com.erpnext.pos.remoteSource.dto.CategoryDto
import com.erpnext.pos.remoteSource.dto.CustomerCreditLimitDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.POSProfileSimpleDto
import com.erpnext.pos.remoteSource.dto.PaymentModesDto
import com.erpnext.pos.remoteSource.dto.UserDto
import com.erpnext.pos.remoteSource.dto.WarehouseItemDto
import com.erpnext.pos.remoteSource.mapper.toBO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlin.jvm.JvmName

@JvmName("toPagingFlowPendingInvoiceBO")
fun Flow<PagingData<SalesInvoiceWithItemsAndPayments>>.toPagingBO(): Flow<PagingData<SalesInvoiceBO>> {
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

@JvmName("ItemEntityToBO")
fun Flow<ItemEntity>.toBO(): Flow<ItemBO> {
    return transform { value ->
        emit(value.toBO())
    }
}

@JvmName("CustomerEntityToBO")
fun Flow<List<CustomerEntity>>.toBO(): Flow<List<CustomerBO>> {
    return transform { value ->
        emit(value.map {
            it.toBO()
        })
    }
}

@JvmName("CustomerDtoToBo")
fun List<CustomerDto>.toBO(): List<CustomerBO> {
    return this.map {
        it.toBO()
    }
}

fun CustomerDto.toBO(): CustomerBO {
    return CustomerBO(
        name = this.name,
        customerName = this.customerName,
        territory = this.territory,
        mobileNo = this.mobileNo,
        customerType = this.customerType,
        creditLimit = this.creditLimits.getOrElse(0, { CustomerCreditLimitDto("", 0.0, false) }).creditLimit,
        totalPendingAmount = this.totalPendingAmount,
        address = address,
        image = image,
        email = email,
        currentBalance = this.totalPendingAmount,
        pendingInvoices = this.pendingInvoicesCount,
        availableCredit = this.availableCredit,
    )
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
        enabled = this.enabled,
    )
}

fun UserEntity.toBO(): UserBO {
    return UserBO(
        name = this.name,
        username = this.username.orEmpty(),
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        language = this.language.orEmpty(),
        enabled = this.enabled,
    )
}

fun DeliveryChargeEntity.toBO(): DeliveryChargeBO {
    return DeliveryChargeBO(
        label = this.label,
        defaultRate = this.defaultRate
    )
}

fun PaymentTermEntity.toBO(): PaymentTermBO {
    return PaymentTermBO(
        name = this.name,
        invoicePortion = this.invoicePortion,
        modeOfPayment = this.modeOfPayment,
        dueDateBasedOn = this.dueDateBasedOn,
        creditDays = this.creditDays,
        creditMonths = this.creditMonths,
        discountType = this.discountType,
        discount = this.discount,
        description = this.description,
        discountValidity = this.discountValidity,
        discountValidityBasedOn = this.discountValidityBasedOn,
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

@JvmName("ItemEntityToBo")
fun List<ItemEntity>.toBO(): List<ItemBO> {
    return this.map { it.toBO() }
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
        lastSyncedAt = this.lastSyncedAt
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

@JvmName("POSProfileSimpleDtoToBOList")
fun List<POSProfileSimpleDto>.toBO(): List<POSProfileSimpleBO> {
    return this.map { it.toBO() }
}

@JvmName("POSProfileSimpleDtoToBO")
fun POSProfileSimpleDto.toBO(): POSProfileSimpleBO {
    return POSProfileSimpleBO(
        name = this.profileName,
        company = this.company,
        currency = this.currency,
        paymentModes = emptyList()
    )
}

@JvmName("SalesInvoiceEntityToBO")
fun SalesInvoiceEntity.toBO(): SalesInvoiceBO {
    return SalesInvoiceBO(
        invoiceId = this.invoiceName ?: "",
        status = this.status,
        paidAmount = this.paidAmount,
        customerPhone = "",
        customer = customerName,
        currency = this.currency,
        docStatus = this.docstatus,
        netTotal = this.netTotal,
        dueDate = this.dueDate,
        outstandingAmount = this.outstandingAmount,
        isPos = false,
        total = this.grandTotal,
        customerId = this.customer,
        postingDate = this.postingDate,
        customExchangeRate = customExchangeRate,
        conversionRate = conversionRate,
        partyAccountCurrency = this.partyAccountCurrency
    )
}

@JvmName("SalesInvoiceWithItemsAndPaymentToBo")
fun List<SalesInvoiceWithItemsAndPayments>.toBO(): List<SalesInvoiceBO> {
    return this.map { it.toBO() }
}

@JvmName("toBOSalesInvoiceWithItemsAndPaymentsEntity")
fun SalesInvoiceWithItemsAndPayments.toBO(): SalesInvoiceBO {
    return SalesInvoiceBO(
        invoiceId = invoice.invoiceName!!,
        customerId = invoice.customer,
        customer = invoice.customerName,
        customerPhone = invoice.customerPhone,
        postingDate = invoice.postingDate,
        dueDate = invoice.dueDate,
        outstandingAmount = invoice.outstandingAmount,
        netTotal = invoice.netTotal,
        total = invoice.grandTotal,
        paidAmount = invoice.paidAmount,
        isPos = invoice.isPos,
        currency = invoice.currency,
        docStatus = invoice.docstatus,
        status = invoice.status,
        syncStatus = invoice.syncStatus,
        items = items.toBO(),
        payments = payments.toBO(),
        customExchangeRate = invoice.customExchangeRate,
        conversionRate = invoice.conversionRate,
        partyAccountCurrency = invoice.partyAccountCurrency
    )
}

@JvmName("SalesInvoiceItemEntityToBOList")
fun List<SalesInvoiceItemEntity>.toBO(): List<SalesInvoiceItemsBO> {
    return this.map { it.toBO() }
}

@JvmName("POSInvoicePaymentEntityToBOList")
fun List<POSInvoicePaymentEntity>.toBO(): List<SalesInvoicePaymentsBO> {
    return this.map { it.toBO() }
}

@JvmName("SalesInvoiceItemEntityToBO")
fun SalesInvoiceItemEntity.toBO(): SalesInvoiceItemsBO {
    return SalesInvoiceItemsBO(
        itemCode = this.itemCode,
        itemName = this.itemName,
        description = this.description,
        uom = this.uom,
        qty = this.qty,
        rate = this.rate,
        amount = this.amount,
        netRate = this.netRate,
        netAmount = this.netAmount
    )
}

@JvmName("POSInvoicePaymentEntityToBO")
fun POSInvoicePaymentEntity.toBO(): SalesInvoicePaymentsBO {
    return SalesInvoicePaymentsBO(
        modeOfPayment = this.modeOfPayment,
        amount = this.amount,
        paymentReference = this.paymentReference,
        paymentDate = this.paymentDate
    )
}

@JvmName("WarehouseItemDtoToBo")
fun List<WarehouseItemDto>.toBO(): List<ItemBO> {
    return this.map { it.toBO() }
}

fun WarehouseItemDto.toBO(): ItemBO {
    return ItemBO(
        name = this.name,
        description = this.description,
        itemCode = this.itemCode,
        barcode = this.barcode,
        image = this.image,
        currency = this.currency,
        itemGroup = this.itemGroup,
        valuationRate = this.valuationRate,
        brand = this.brand,
        price = this.price,
        actualQty = this.actualQty,
        discount = this.discount,
        isService = this.isService,
        isStocked = this.isStocked,
        uom = this.stockUom
    )
}
