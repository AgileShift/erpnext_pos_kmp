package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.InventoryBinEntity
import com.erpnext.pos.localSource.entities.v2.ItemEntity
import com.erpnext.pos.localSource.entities.v2.ItemGroupEntity
import com.erpnext.pos.localSource.entities.v2.ItemPriceEntity
import com.erpnext.pos.remoteSource.dto.v2.BinDto
import com.erpnext.pos.remoteSource.dto.v2.ItemDto
import com.erpnext.pos.remoteSource.dto.v2.ItemGroupDto
import com.erpnext.pos.remoteSource.dto.v2.ItemPriceDto

fun ItemDto.toEntity(instanceId: String, companyId: String) =
    ItemEntity(
        itemId = itemCode,
        itemCode = itemCode,
        itemName = itemName,
        itemGroup = itemGroup,
        brand = brand,
        stockUom = stockUom,
        salesUom = salesUom ?: stockUom,
        isStockItem = isStockItem,
        allowNegativeStock = allowNegativeStock,
        imageUrl = imageUrl,
        disabled = disabled
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun ItemGroupDto.toEntity(instanceId: String, companyId: String) =
    ItemGroupEntity(
        itemGroupId = itemGroupId,
        itemGroupName = itemGroupName ?: itemGroupId,
        parentItemGroupId = parentItemGroupId,
        isGroup = isGroup
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun ItemPriceDto.toEntity(instanceId: String, companyId: String) =
    ItemPriceEntity(
        itemPriceId = itemPriceId,
        itemId = itemCode,
        priceList = priceList,
        rate = priceListRate.toFloat(),
        currency = currency
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }

fun BinDto.toEntity(instanceId: String, companyId: String) =
    InventoryBinEntity(
        itemId = itemCode,
        warehouseId = warehouse,
        actualQty = actualQty.toFloat(),
        projectedQty = projectedQty?.toFloat(),
        reservedQty = reservedQty?.toFloat()
    ).apply {
        this.instanceId = instanceId
        this.companyId = companyId
    }
