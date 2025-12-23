package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.PaymentModesEntity
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.PaymentModesDto
import kotlin.jvm.JvmName

//TODO: Verificar si las propiedades son las correctas y estan todas mapeadas
@JvmName("toEntityPOSProfileDto")
fun List<POSProfileDto>.toEntity(): List<POSProfileEntity> {
    return this.map { it.toEntity() }
}

fun POSProfileDto.toEntity(): POSProfileEntity {
    return POSProfileEntity(
        profileName = this.profileName,
        warehouse = this.warehouse,
        country = this.country,
        company = this.company,
        currency = this.currency,
        route = this.route,
        user = "",
        incomeAccount = this.incomeAccount,
        expenseAccount = this.expenseAccount,
        branch = this.branch,
        applyDiscountOn = this.applyDiscountOn,
        costCenter = this.costCenter,
        sellingPriceList = this.sellingPriceList,
    )
}

@JvmName("ListPaymentModesDtoToEntity")
fun List<PaymentModesDto>.toEntity(): List<PaymentModesEntity> {
    return this.map { it.toEntity() }
}

fun PaymentModesDto.toEntity(): PaymentModesEntity {
    return PaymentModesEntity(
        name = this.name,
        default = this.default,
        modeOfPayment = this.modeOfPayment
    )
}

@JvmName("ListItemBoToEntity")
fun List<ItemBO>.toEntity(): List<ItemEntity> {
    return this.map { it.toEntity() }
}

fun ItemBO.toEntity(): ItemEntity {
    return ItemEntity(
        itemCode = this.itemCode,
        actualQty = this.actualQty,
        name = this.name,
        description = this.description,
        barcode = this.barcode,
        image = this.image,
        itemGroup = this.itemGroup,
        brand = this.brand,
        price = this.price,
        discount = this.discount,
        isService = this.isService,
        isStocked = this.isStocked,
        stockUom = this.uom,
        currency = this.currency ?: ""
    )
}

fun POSProfileEntity.toOpeningEntry(cashbox: CashboxEntity) : POSOpeningEntryEntity? {
    return null
}

fun POSProfileEntity.toClosingEntry(cashbox: CashboxEntity) : POSClosingEntryEntity? {
    return null
}