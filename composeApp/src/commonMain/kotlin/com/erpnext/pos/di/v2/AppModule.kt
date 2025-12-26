package com.erpnext.pos.di.v2

import com.erpnext.pos.data.AppDatabase
import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.data.repositories.v2.CatalogSyncRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.DeliveryNoteRepository
import com.erpnext.pos.data.repositories.v2.InventoryRepository
import com.erpnext.pos.data.repositories.v2.PaymentEntryRepository
import com.erpnext.pos.data.repositories.v2.QuotationRepository
import com.erpnext.pos.data.repositories.v2.SalesOrderRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.usecases.v2.CreateCustomerOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreateDeliveryNoteOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreatePaymentEntryOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreateQuotationOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.CreateSalesOrderOfflineUseCase
import com.erpnext.pos.domain.usecases.v2.sync.CustomerSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.DeliveryNoteSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.PaymentEntrySyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.QuotationSyncUnit
import com.erpnext.pos.domain.usecases.v2.sync.SalesOrderSyncUnit
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.domain.utils.UUIDGenerator
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

    single { CatalogRepository(get()) }
    single { CatalogSyncRepository(get(), get()) }
    single { InventoryRepository(get()) }
    single { CustomerRepository(get(), get(), get(), get(), get(), get()) }
    single { QuotationRepository(get(), get()) }
    single { SalesOrderRepository(get(), get()) }
    single { DeliveryNoteRepository(get(), get()) }
    single { PaymentEntryRepository(get(), get()) }
    single { SyncRepository(get(), get()) }
    single { APIServiceV2(get(), get(), get()) }

    factory { UUIDGenerator() }

    single { CreateQuotationOfflineUseCase(get(), get(), get(), get(), get()) }
    single { CreateSalesOrderOfflineUseCase(get(), get(), get(), get(), get()) }
    single { CreateDeliveryNoteOfflineUseCase(get(), get(), get(), get(), get()) }
    single { CreatePaymentEntryOfflineUseCase(get(), get(), get()) }
    single { CreateCustomerOfflineUseCase(get(), get()) }

    factory { CustomerSyncUnit(get(), get(), get()) }
    factory { QuotationSyncUnit(get(), get(), get()) }
    factory { SalesOrderSyncUnit(get(), get(), get()) }
    factory { DeliveryNoteSyncUnit(get(), get(), get()) }
    factory { PaymentEntrySyncUnit(get(), get(), get()) }
}
