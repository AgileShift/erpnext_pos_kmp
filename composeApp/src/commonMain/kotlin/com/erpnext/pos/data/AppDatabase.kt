package com.erpnext.pos.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.DeliveryChargeDao
import com.erpnext.pos.localSource.dao.ExchangeRateDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.PaymentTermDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
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
import com.erpnext.pos.localSource.entities.POSOpeningEntryLinkEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import com.erpnext.pos.localSource.entities.PaymentTermEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.TaxDetailsEntity
import com.erpnext.pos.localSource.entities.UserEntity
import com.erpnext.pos.localSource.entities.CompanyEntity
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
        PosProfilePaymentMethodEntity::class,
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
        POSOpeningEntryLinkEntity::class,
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
    version = 25,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23)
    ]
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun posProfileDao(): POSProfileDao
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
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect class DatabaseBuilder {
    fun build(): AppDatabase
}

object AppDatabaseMigrations {
    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tabPosProfilePaymentMethod` (
                    `profile_id` TEXT NOT NULL,
                    `mode_of_payment` TEXT NOT NULL,
                    `company` TEXT NOT NULL,
                    `is_default` INTEGER NOT NULL,
                    `allow_in_returns` INTEGER NOT NULL,
                    `idx` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    PRIMARY KEY(`profile_id`, `mode_of_payment`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `tabPosProfilePaymentMethod` (
                    `profile_id`,
                    `mode_of_payment`,
                    `company`,
                    `is_default`,
                    `allow_in_returns`,
                    `idx`,
                    `enabled`
                )
                SELECT
                    pm.profileId,
                    pm.mode_of_payment,
                    COALESCE(pp.company, ''),
                    pm.`default`,
                    0,
                    0,
                    1
                FROM tabPaymentModes pm
                LEFT JOIN tabPosProfile pp
                  ON pp.profile_name = pm.profileId
                """.trimIndent()
            )
            db.execSQL("DROP TABLE IF EXISTS tabPaymentModes")
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tab_pos_opening_entry_link` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `cashbox_id` INTEGER NOT NULL,
                    `local_opening_entry_name` TEXT NOT NULL,
                    `remote_opening_entry_name` TEXT,
                    `pending_sync` INTEGER NOT NULL,
                    FOREIGN KEY(`cashbox_id`) REFERENCES `tabCashbox`(`localId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`local_opening_entry_name`) REFERENCES `tab_pos_opening_entry`(`name`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_tab_pos_opening_entry_link_cashbox_id`
                ON `tab_pos_opening_entry_link` (`cashbox_id`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_tab_pos_opening_entry_link_local_opening_entry_name`
                ON `tab_pos_opening_entry_link` (`local_opening_entry_name`)
                """.trimIndent()
            )
        }
    }
}
