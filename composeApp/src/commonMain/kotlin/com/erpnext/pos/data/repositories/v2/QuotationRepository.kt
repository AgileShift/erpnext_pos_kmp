package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.PendingSync
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.QuotationDao
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.QuotationCreateDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationItemCreateDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationSnapshot
import com.erpnext.pos.remoteSource.dto.v2.QuotationTaxCreateDto
import com.erpnext.pos.remoteSource.mapper.v2.toEntity
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.IncrementalSyncFilters
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class QuotationRepository(
    private val quotationDao: QuotationDao,
    private val customerRepository: CustomerRepository,
    private val api: APIServiceV2
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        val quotations = api.list<QuotationSnapshot>(
            doctype = ERPDocType.Quotation,
            filters = IncrementalSyncFilters.quotation(ctx)
        )
        if (quotations.isEmpty()) return false
        val entities = quotations.map { it.toEntity(ctx.instanceId, ctx.companyId) }
        quotationDao.upsertQuotations(entities)
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<PendingSync<QuotationCreateDto>> {
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

    @OptIn(ExperimentalTime::class)
    suspend fun markSynced(
        instanceId: String,
        companyId: String,
        quotationId: String,
        remoteName: String?,
        remoteModified: String?
    ) {
        val now = Clock.System.now().epochSeconds
        quotationDao.updateSyncStatus(
            instanceId,
            companyId,
            quotationId,
            syncStatus = "SYNCED",
            lastSyncedAt = now,
            updatedAt = now
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markFailed(
        instanceId: String,
        companyId: String,
        quotationId: String
    ) {
        val now = Clock.System.now().epochSeconds
        quotationDao.updateSyncStatus(
            instanceId,
            companyId,
            quotationId,
            syncStatus = "FAILED",
            lastSyncedAt = null,
            updatedAt = now
        )
    }

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<PendingSync<QuotationCreateDto>> {
        return quotationDao.getPendingQuotationsWithDetails(instanceId, companyId).map { snapshot ->
            val customerId = customerRepository.resolveRemoteCustomerId(
                instanceId,
                companyId,
                snapshot.quotation.partyName
            )
            PendingSync(
                localId = snapshot.quotation.quotationId,
                payload = QuotationCreateDto(
                    company = snapshot.quotation.company,
                    transactionDate = snapshot.quotation.transactionDate,
                    partyName = customerId,
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
            )
        }
    }
}
