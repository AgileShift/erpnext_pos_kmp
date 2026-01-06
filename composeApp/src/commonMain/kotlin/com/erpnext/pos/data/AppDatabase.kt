package com.erpnext.pos.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.DeliveryChargeDao
import com.erpnext.pos.localSource.dao.ExchangeRateDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.PaymentTermDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PaymentModesDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
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
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.DeliveryChargeEntity
import com.erpnext.pos.localSource.entities.ExchangeRateEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.PaymentModesEntity
import com.erpnext.pos.localSource.entities.PaymentTermEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.TaxDetailsEntity
import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.localSource.entities.v2.CompanyEntity
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
import com.erpnext.pos.localSource.entities.v2.TerritoryEntity
import com.erpnext.pos.localSource.entities.v2.UserEntity as UserEntityV2

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        POSProfileEntity::class,
        PaymentModesEntity::class,
        PaymentTermEntity::class,
        DeliveryChargeEntity::class,
        ExchangeRateEntity::class,
        ModeOfPaymentEntity::class,
        POSInvoicePaymentEntity::class,
        CashboxEntity::class,
        BalanceDetailsEntity::class,
        CustomerEntity::class,
        CategoryEntity::class,
        SalesInvoiceEntity::class,
        SalesInvoiceItemEntity::class,
        POSOpeningEntryEntity::class,
        POSClosingEntryEntity::class,
        TaxDetailsEntity::class,
        UserEntityV2::class,
        CompanyEntity::class,
        EmployeeEntity::class,
        SalesPersonEntity::class,
        SalesTeamEntity::class,
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
        SyncStateEntity::class
    ],
    version = 18,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 16,
            to = 17
        ),
        AutoMigration(
            from = 17,
            to = 18
        )
    ]
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun posProfileDao(): POSProfileDao
    abstract fun paymentModesDao(): PaymentModesDao
    abstract fun modeOfPaymentDao(): ModeOfPaymentDao
    abstract fun cashboxDao(): CashboxDao
    abstract fun customerDao(): CustomerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleInvoiceDao(): SalesInvoiceDao
    abstract fun posOpeningDao(): POSOpeningEntryDao
    abstract fun posClosingDao(): POSClosingEntryDao
    abstract fun exchangeRateDao(): ExchangeRateDao
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
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect class DatabaseBuilder {
    fun build(): AppDatabase
}
