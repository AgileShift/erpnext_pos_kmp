package com.erpnext.pos.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.dao.AddressDao
import com.erpnext.pos.localSource.dao.ContactDao
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.CustomerOutboxDao
import com.erpnext.pos.localSource.dao.ConfigurationDao
import com.erpnext.pos.localSource.dao.CustomerGroupDao
import com.erpnext.pos.localSource.dao.DeliveryChargeDao
import com.erpnext.pos.localSource.dao.ExchangeRateDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.ItemReorderDao
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.PaymentTermDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.TerritoryDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.dao.v2.CatalogDao as CatalogDaoV2
import com.erpnext.pos.localSource.dao.v2.CustomerDao as CustomerDaoV2
import com.erpnext.pos.localSource.dao.v2.DeliveryNoteDao
import com.erpnext.pos.localSource.dao.v2.InventoryDao as InventoryDaoV2
import com.erpnext.pos.localSource.dao.v2.PaymentScheduleDao
import com.erpnext.pos.localSource.dao.v2.PaymentEntryDao
import com.erpnext.pos.localSource.dao.v2.POSContextDao
import com.erpnext.pos.localSource.dao.v2.PricingRuleDao
import com.erpnext.pos.localSource.dao.v2.QuotationDao
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao as SalesInvoiceDaoV2
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceItemDao
import com.erpnext.pos.localSource.dao.v2.SalesOrderDao
import com.erpnext.pos.localSource.dao.v2.SyncStatusDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.AddressEntity
import com.erpnext.pos.localSource.entities.CustomerGroupEntity
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.CustomerOutboxEntity
import com.erpnext.pos.localSource.entities.DeliveryChargeEntity
import com.erpnext.pos.localSource.entities.ExchangeRateEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.ItemReorderEntity
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryLinkEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.PosProfileLocalEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import com.erpnext.pos.localSource.entities.PaymentTermEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.TerritoryEntity
import com.erpnext.pos.localSource.entities.TaxDetailsEntity
import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.ContactEntity
import com.erpnext.pos.localSource.entities.ConfigurationEntity
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity as CustomerEntityV2
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
import com.erpnext.pos.localSource.entities.v2.EmployeeEntity
import com.erpnext.pos.localSource.entities.v2.InventoryBinEntity
import com.erpnext.pos.localSource.entities.v2.ItemEntity as ItemEntityV2
import com.erpnext.pos.localSource.entities.v2.ItemGroupEntity
import com.erpnext.pos.localSource.entities.v2.ItemPriceEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.localSource.entities.v2.PaymentScheduleEntity
import com.erpnext.pos.localSource.entities.v2.POSPaymentMethodEntity
import com.erpnext.pos.localSource.entities.v2.POSProfileEntity as POSProfileEntityV2
import com.erpnext.pos.localSource.entities.v2.PricingRuleEntity
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.localSource.entities.v2.RouteEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity as SalesInvoiceEntityV2
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity as SalesInvoiceItemEntityV2
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesPersonEntity
import com.erpnext.pos.localSource.entities.v2.SalesTaxAndChargeEntity
import com.erpnext.pos.localSource.entities.v2.SalesTeamEntity
import com.erpnext.pos.localSource.entities.v2.SyncStateEntity
import com.erpnext.pos.localSource.entities.v2.TerritoryEntity as TerritoryEntityV2
import com.erpnext.pos.localSource.entities.v2.UserEntity as UserEntityV2

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        ItemReorderEntity::class,
        POSProfileEntity::class,
        PosProfileLocalEntity::class,
        PosProfilePaymentMethodEntity::class,
        PaymentTermEntity::class,
        DeliveryChargeEntity::class,
        ExchangeRateEntity::class,
        ModeOfPaymentEntity::class,
        POSInvoicePaymentEntity::class,
        CashboxEntity::class,
        BalanceDetailsEntity::class,
        CustomerEntity::class,
        ContactEntity::class,
        AddressEntity::class,
        CustomerGroupEntity::class,
        ConfigurationEntity::class,
        CategoryEntity::class,
        SalesInvoiceEntity::class,
        SalesInvoiceItemEntity::class,
        POSOpeningEntryEntity::class,
        POSOpeningEntryLinkEntity::class,
        POSClosingEntryEntity::class,
        TaxDetailsEntity::class,
        UserEntityV2::class,
        CompanyEntity::class,
        EmployeeEntity::class,
        SalesPersonEntity::class,
        SalesTeamEntity::class,
        TerritoryEntityV2::class,
        TerritoryEntity::class,
        //====== V2 ======
        CustomerEntityV2::class,
        CustomerAddressEntity::class,
        CustomerContactEntity::class,
        RouteEntity::class,
        POSProfileEntityV2::class,
        POSPaymentMethodEntity::class,
        ItemEntityV2::class,
        ItemGroupEntity::class,
        ItemPriceEntity::class,
        InventoryBinEntity::class,
        PricingRuleEntity::class,
        SalesInvoiceEntityV2::class,
        SalesInvoiceItemEntityV2::class,
        SalesInvoicePaymentEntity::class,
        SalesTaxAndChargeEntity::class,
        QuotationEntity::class,
        QuotationItemEntity::class,
        QuotationTaxEntity::class,
        QuotationCustomerLinkEntity::class,
        SalesOrderEntity::class,
        SalesOrderItemEntity::class,
        DeliveryNoteEntity::class,
        DeliveryNoteItemEntity::class,
        DeliveryNoteLinkEntity::class,
        PaymentEntryEntity::class,
        PaymentEntryReferenceEntity::class,
        PaymentScheduleEntity::class,
        SyncStateEntity::class,
        CustomerOutboxEntity::class
    ],
    version = 3,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun itemReorderDao(): ItemReorderDao
    abstract fun posProfileDao(): POSProfileDao
    abstract fun posProfileLocalDao(): PosProfileLocalDao
    abstract fun posProfilePaymentMethodDao(): PosProfilePaymentMethodDao
    abstract fun modeOfPaymentDao(): ModeOfPaymentDao
    abstract fun cashboxDao(): CashboxDao
    abstract fun customerDao(): CustomerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleInvoiceDao(): SalesInvoiceDao
    abstract fun posOpeningDao(): POSOpeningEntryDao
    abstract fun posOpeningEntryLinkDao(): POSOpeningEntryLinkDao
    abstract fun posClosingDao(): POSClosingEntryDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun configurationDao(): ConfigurationDao
    abstract fun catalogDaoV2(): CatalogDaoV2
    abstract fun customerDaoV2(): CustomerDaoV2
    abstract fun inventoryDaoV2(): InventoryDaoV2
    abstract fun posContextDaoV2(): POSContextDao
    abstract fun salesInvoiceDaoV2(): SalesInvoiceDaoV2
    abstract fun salesInvoiceItemDaoV2(): SalesInvoiceItemDao
    abstract fun syncStatusDaoV2(): SyncStatusDao
    abstract fun quotationDaoV2(): QuotationDao
    abstract fun salesOrderDaoV2(): SalesOrderDao
    abstract fun deliveryNoteDaoV2(): DeliveryNoteDao
    abstract fun paymentEntryDaoV2(): PaymentEntryDao
    abstract fun paymentScheduleDaoV2(): PaymentScheduleDao
    abstract fun pricingRuleDaoV2(): PricingRuleDao

    abstract fun paymentTermDao(): PaymentTermDao
    abstract fun deliveryChargeDao(): DeliveryChargeDao
    abstract fun companyDao(): CompanyDao
    abstract fun customerOutboxDao(): CustomerOutboxDao
    abstract fun customerGroupDao(): CustomerGroupDao
    abstract fun territoryDao(): TerritoryDao
    abstract fun contactDao(): ContactDao
    abstract fun addressDao(): AddressDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect class DatabaseBuilder {
    fun build(): AppDatabase
}
