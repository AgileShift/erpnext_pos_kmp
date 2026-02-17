package com.erpnext.pos.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.erpnext.pos.localSource.dao.AddressDao
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.dao.ConfigurationDao
import com.erpnext.pos.localSource.dao.ContactDao
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.CustomerGroupDao
import com.erpnext.pos.localSource.dao.CustomerOutboxDao
import com.erpnext.pos.localSource.dao.DeliveryChargeDao
import com.erpnext.pos.localSource.dao.ExchangeRateDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.ItemReorderDao
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PaymentTermDao
import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.TerritoryDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.AddressEntity
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.ConfigurationEntity
import com.erpnext.pos.localSource.entities.ContactEntity
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.CustomerGroupEntity
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
import com.erpnext.pos.localSource.entities.PaymentTermEntity
import com.erpnext.pos.localSource.entities.PosProfileLocalEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.TaxDetailsEntity
import com.erpnext.pos.localSource.entities.TerritoryEntity
import com.erpnext.pos.localSource.entities.UserEntity

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
        CompanyEntity::class,
        TerritoryEntity::class,
        CustomerOutboxEntity::class,
    ],
    version = 11,
    autoMigrations = [
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10)
    ],
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
