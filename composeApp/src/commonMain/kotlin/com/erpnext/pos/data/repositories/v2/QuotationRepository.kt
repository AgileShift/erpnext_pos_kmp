package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.QuotationDao
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.remoteSource.dto.v2.QuotationCreateDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationItemCreateDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationTaxCreateDto

class QuotationRepository(
    private val quotationDao: QuotationDao
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<QuotationCreateDto> {
        return buildPendingCreatePayloads(ctx.instanceId, ctx.companyId)
    }

    suspend fun insertQuotationWithDetails(
        quotation: QuotationEntity,
        items: List<QuotationItemEntity>,
        taxes: List<QuotationTaxEntity>,
        customerLinks: List<QuotationCustomerLinkEntity>
    ) {
        quotationDao.insertQuotationWithDetails(quotation, items, taxes, customerLinks)
    }

    suspend fun getPendingQuotationsWithDetails(
        instanceId: String,
        companyId: String
    ) = quotationDao.getPendingQuotationsWithDetails(instanceId, companyId)

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<QuotationCreateDto> {
        return quotationDao.getPendingQuotationsWithDetails(instanceId, companyId).map { snapshot ->
            QuotationCreateDto(
                company = snapshot.quotation.company,
                transactionDate = snapshot.quotation.transactionDate,
                partyName = snapshot.quotation.partyName,
                validTill = snapshot.quotation.validUntil,
                customerName = snapshot.quotation.customerName,
                territory = snapshot.quotation.territory,
                sellingPriceList = snapshot.quotation.sellingPriceList,
                currency = snapshot.quotation.priceListCurrency,
                items = snapshot.items.map { item ->
                    QuotationItemCreateDto(
                        itemCode = item.itemCode,
                        qty = item.qty,
                        rate = item.rate,
                        uom = item.uom,
                        warehouse = item.warehouse
                    )
                },
                taxes = snapshot.taxes.map { tax ->
                    QuotationTaxCreateDto(
                        chargeType = tax.chargeType,
                        accountHead = tax.accountHead,
                        rate = tax.rate,
                        taxAmount = tax.taxAmount
                    )
                }
            )
        }
    }
}
