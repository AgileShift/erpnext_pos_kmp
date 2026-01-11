package com.erpnext.pos.di.v2

import com.erpnext.pos.data.adapters.local.SalesInvoiceLocalAdapter
import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.data.repositories.v2.CatalogSyncRepository
import com.erpnext.pos.data.repositories.v2.ContextRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.DeliveryNoteRepository
import com.erpnext.pos.data.repositories.v2.DeliveryChargesRepository
import com.erpnext.pos.data.repositories.v2.InventoryRepository
import com.erpnext.pos.data.repositories.v2.PaymentEntryRepository
import com.erpnext.pos.data.repositories.v2.QuotationRepository
import com.erpnext.pos.data.repositories.v2.SalesInvoiceRemoteRepository
import com.erpnext.pos.data.repositories.v2.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.v2.SalesOrderRepository
import com.erpnext.pos.data.repositories.v2.SourceDocumentRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.sync.SyncUnit
import com.erpnext.pos.domain.usecases.v2.CreateCustomerOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreateDeliveryNoteOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreatePaymentEntryOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreateQuotationOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreateSalesOrderOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.LoadSourceDocumentsUseCase
import com.erpnext.pos.domain.usecases.v2.sync.BinSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.CustomerSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.DeliveryNoteSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.ItemGroupSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.ItemPriceSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.ItemSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.PaymentEntrySyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.QuotationSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.SalesInvoiceSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.SalesOrderSyncUnit
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.domain.utils.UUIDGenerator
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModulev2 = module {
    single { get<AppDatabase>().catalogDaoV2() }
    single { get<AppDatabase>().inventoryDaoV2() }
    single { get<AppDatabase>().customerDaoV2() }
    single { get<AppDatabase>().salesInvoiceDaoV2() }
    single { get<AppDatabase>().syncStatusDaoV2() }
    single { get<AppDatabase>().quotationDaoV2() }
    single { get<AppDatabase>().salesOrderDaoV2() }
    single { get<AppDatabase>().deliveryNoteDaoV2() }
    single { get<AppDatabase>().paymentEntryDaoV2() }
    single { get<AppDatabase>().paymentScheduleDaoV2() }
    single { get<AppDatabase>().posContextDaoV2() }

    single { CatalogRepository(get()) }
    single { CatalogSyncRepository(get(), get()) }
    single { ContextRepository(get(), get()) }
    single { InventoryRepository(get()) }
    single { CustomerRepository(get(), get(), get(), get(), get(), get(), get()) }
    single { SalesInvoiceLocalAdapter(get()) }
    single { SalesInvoiceRemoteRepository(get(), get()) }
    single { SalesInvoiceRepository(get(), get(), get(), get()) }
    single { QuotationRepository(get(), get(), get(), get()) }
    single { SalesOrderRepository(get(), get(), get(), get()) }
    single { DeliveryNoteRepository(get(), get(), get()) }
    single { SourceDocumentRepository(get(named("apiServiceV2"))) }
    single { DeliveryChargesRepository(get(named("apiServiceV2"))) }
    single { PaymentEntryRepository(get(), get(), get()) }
    single { SyncRepository(get(), get()) }

    factory { UUIDGenerator() }

    single { CreateQuotationOfflineUseCase(get(), get(), get(), get(), get()) }
    single { CreateSalesOrderOfflineUseCase(get(), get(), get(), get(), get()) }
    single { CreateDeliveryNoteOfflineUseCase(get(), get(), get(), get(), get()) }
    single { CreatePaymentEntryOfflineUseCase(get(), get(), get()) }
    single { CreateCustomerOfflineUseCase(get(), get()) }
    single { LoadSourceDocumentsUseCase(get()) }

    factory { ItemGroupSyncUnit(get(), get()) }
    factory { ItemSyncUnit(get(), get()) }
    factory { ItemPriceSyncUnit(get(), get()) }
    factory { BinSyncUnit(get(), get()) }
    factory { CustomerSyncUnit(get(), get(named("apiServiceV2")), get()) }
    factory { SalesInvoiceSyncUnit(get(), get()) }
    factory { QuotationSyncUnit(get(), get(named("apiServiceV2")), get()) }
    factory { SalesOrderSyncUnit(get(), get(named("apiServiceV2")), get()) }
    factory { DeliveryNoteSyncUnit(get(), get(named("apiServiceV2")), get()) }
    factory { PaymentEntrySyncUnit(get(), get(named("apiServiceV2")), get()) }

    factory<List<SyncUnit>> {
        listOf(
            get<ItemGroupSyncUnit>(),
            get<ItemSyncUnit>(),
            get<ItemPriceSyncUnit>(),
            get<BinSyncUnit>(),
            get<CustomerSyncUnit>(),
            get<SalesInvoiceSyncUnit>(),
            get<QuotationSyncUnit>(),
            get<SalesOrderSyncUnit>(),
            get<DeliveryNoteSyncUnit>(),
            get<PaymentEntrySyncUnit>()
        )
    }
}
