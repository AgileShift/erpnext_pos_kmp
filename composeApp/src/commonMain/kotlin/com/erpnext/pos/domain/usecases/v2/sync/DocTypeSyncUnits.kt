package com.erpnext.pos.domain.usecases.v2.sync

import com.erpnext.pos.data.repositories.v2.CatalogSyncRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.DeliveryNoteRepository
import com.erpnext.pos.data.repositories.v2.PaymentEntryRepository
import com.erpnext.pos.data.repositories.v2.QuotationRepository
import com.erpnext.pos.data.repositories.v2.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.v2.SalesOrderRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.sync.SyncDocType
import com.erpnext.pos.domain.sync.SyncUnit
import com.erpnext.pos.domain.sync.SyncUnitResult
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.CustomerCreateDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationCreateDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderCreateDto
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.utils.toErpDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private suspend fun SyncRepository.runDocType(
    ctx: SyncContext,
    docType: SyncDocType,
    pull: suspend () -> Boolean,
    push: suspend () -> Boolean
): SyncUnitResult {
    val docTypeValue = docType.value
    setInProgress(ctx.instanceId, ctx.companyId, docTypeValue, true)
    return try {
        val pulled = pull()
        markPullSuccess(ctx.instanceId, ctx.companyId, docTypeValue)
        val pushed = push()
        if (pushed) {
            markPushSuccess(ctx.instanceId, ctx.companyId, docTypeValue)
        }
        refreshCounters(ctx.instanceId, ctx.companyId, docTypeValue, Clock.System.now().epochSeconds)
        SyncUnitResult(success = true, changed = pulled || pushed)
    } catch (e: Throwable) {
        markFailure(ctx.instanceId, ctx.companyId, docTypeValue, e)
        SyncUnitResult(success = false, changed = false, error = e)
    } finally {
        setInProgress(ctx.instanceId, ctx.companyId, docTypeValue, false)
    }
}

class ItemGroupSyncUnit(
    private val repository: CatalogSyncRepository,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.ITEM_GROUP.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.ITEM_GROUP,
            pull = {
                val modifiedSince = syncRepository
                    .getLastPullAt(ctx.instanceId, ctx.companyId, SyncDocType.ITEM_GROUP.value)
                    ?.let { (it * 1000).toErpDateTime() }
                repository.syncItemGroups(ctx, modifiedSince) > 0
            },
            push = { false }
        )
    }
}

class ItemSyncUnit(
    private val repository: CatalogSyncRepository,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.ITEM.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.ITEM,
            pull = {
                val modifiedSince = syncRepository
                    .getLastPullAt(ctx.instanceId, ctx.companyId, SyncDocType.ITEM.value)
                    ?.let { (it * 1000).toErpDateTime() }
                repository.syncItems(ctx, modifiedSince) > 0
            },
            push = { false }
        )
    }
}

class ItemPriceSyncUnit(
    private val repository: CatalogSyncRepository,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.ITEM_PRICE.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.ITEM_PRICE,
            pull = {
                val modifiedSince = syncRepository
                    .getLastPullAt(ctx.instanceId, ctx.companyId, SyncDocType.ITEM_PRICE.value)
                    ?.let { (it * 1000).toErpDateTime() }
                repository.syncItemPrices(ctx, modifiedSince) > 0
            },
            push = { false }
        )
    }
}

class BinSyncUnit(
    private val repository: CatalogSyncRepository,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.BIN.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.BIN,
            pull = {
                val modifiedSince = syncRepository
                    .getLastPullAt(ctx.instanceId, ctx.companyId, SyncDocType.BIN.value)
                    ?.let { (it * 1000).toErpDateTime() }
                repository.syncBins(ctx, modifiedSince) > 0
            },
            push = { false }
        )
    }
}

class CustomerSyncUnit(
    private val repository: CustomerRepository,
    private val api: APIServiceV2,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.CUSTOMER.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.CUSTOMER,
            pull = { repository.pull(ctx) },
            push = {
                val pending = repository.pushPending(ctx)
                var hadFailure = false
                pending.forEach { item ->
                    try {
                        val response =
                            api.createDoc<CustomerCreateDto>(ERPDocType.Customer, item.payload)
                        repository.markCustomerSynced(
                            ctx.instanceId,
                            ctx.companyId,
                            item.localId,
                            response.name,
                            response.modified
                        )
                    } catch (e: Throwable) {
                        repository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                        hadFailure = true
                    }
                }
                if (hadFailure) {
                    throw IllegalStateException("Customer sync had failures.")
                }
                pending.isNotEmpty()
            }
        )
    }
}

class SalesInvoiceSyncUnit(
    private val repository: SalesInvoiceRepository,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.SALES_INVOICE.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.SALES_INVOICE,
            pull = { repository.pullInvoices(ctx) },
            push = { repository.syncOutbox(ctx) }
        )
    }
}

class QuotationSyncUnit(
    private val repository: QuotationRepository,
    private val api: APIServiceV2,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.QUOTATION.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.QUOTATION,
            pull = { repository.pull(ctx) },
            push = {
                val pending = repository.pushPending(ctx)
                var hadFailure = false
                pending.forEach { item ->
                    try {
                        val response =
                            api.createDoc<QuotationCreateDto>(ERPDocType.Quotation, item.payload)
                        repository.markSynced(
                            ctx.instanceId,
                            ctx.companyId,
                            item.localId,
                            response.name,
                            response.modified
                        )
                    } catch (e: Throwable) {
                        repository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                        hadFailure = true
                    }
                }
                if (hadFailure) {
                    throw IllegalStateException("Quotation sync had failures.")
                }
                pending.isNotEmpty()
            }
        )
    }
}

class SalesOrderSyncUnit(
    private val repository: SalesOrderRepository,
    private val api: APIServiceV2,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.SALES_ORDER.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.SALES_ORDER,
            pull = { repository.pull(ctx) },
            push = {
                val pending = repository.pushPending(ctx)
                var hadFailure = false
                pending.forEach { item ->
                    try {
                        val response =
                            api.createDoc<SalesOrderCreateDto>(ERPDocType.SalesOrder, item.payload)
                        repository.markSynced(
                            ctx.instanceId,
                            ctx.companyId,
                            item.localId,
                            response.name,
                            response.modified
                        )
                    } catch (e: Throwable) {
                        repository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                        hadFailure = true
                    }
                }
                if (hadFailure) {
                    throw IllegalStateException("Sales Order sync had failures.")
                }
                pending.isNotEmpty()
            }
        )
    }
}

class DeliveryNoteSyncUnit(
    private val repository: DeliveryNoteRepository,
    private val api: APIServiceV2,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.DELIVERY_NOTE.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.DELIVERY_NOTE,
            pull = { repository.pull(ctx) },
            push = {
                val pending = repository.pushPending(ctx)
                var hadFailure = false
                pending.forEach { item ->
                    try {
                        val response = api.createDoc<DeliveryNoteCreateDto>(
                            ERPDocType.DeliveryNote,
                            item.payload
                        )
                        repository.markSynced(
                            ctx.instanceId,
                            ctx.companyId,
                            item.localId,
                            response.name,
                            response.modified
                        )
                    } catch (e: Throwable) {
                        repository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                        hadFailure = true
                    }
                }
                if (hadFailure) {
                    throw IllegalStateException("Delivery Note sync had failures.")
                }
                pending.isNotEmpty()
            }
        )
    }
}

class PaymentEntrySyncUnit(
    private val repository: PaymentEntryRepository,
    private val api: APIServiceV2,
    private val syncRepository: SyncRepository
) : SyncUnit {
    override val name = SyncDocType.PAYMENT_ENTRY.value

    override suspend fun run(ctx: SyncContext): SyncUnitResult {
        return syncRepository.runDocType(
            ctx = ctx,
            docType = SyncDocType.PAYMENT_ENTRY,
            pull = { repository.pull(ctx) },
            push = {
                val pending = repository.pushPending(ctx)
                var hadFailure = false
                pending.forEach { item ->
                    try {
                        val response = api.createDoc<PaymentEntryCreateDto>(
                            ERPDocType.PaymentEntry,
                            item.payload
                        )
                        repository.markSynced(
                            ctx.instanceId,
                            ctx.companyId,
                            item.localId,
                            response.name,
                            response.modified
                        )
                    } catch (e: Throwable) {
                        repository.markFailed(ctx.instanceId, ctx.companyId, item.localId)
                        hadFailure = true
                    }
                }
                if (hadFailure) {
                    throw IllegalStateException("Payment Entry sync had failures.")
                }
                pending.isNotEmpty()
            }
        )
    }
}
