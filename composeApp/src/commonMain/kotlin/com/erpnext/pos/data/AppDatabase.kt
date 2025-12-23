package com.erpnext.pos.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PaymentModesDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.PaymentModesEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.TaxDetailsEntity
import com.erpnext.pos.localSource.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        POSProfileEntity::class,
        PaymentModesEntity::class,
        POSInvoicePaymentEntity::class,
        CashboxEntity::class,
        BalanceDetailsEntity::class,
        CustomerEntity::class,
        CategoryEntity::class,
        SalesInvoiceEntity::class,
        SalesInvoiceItemEntity::class,
        POSOpeningEntryEntity::class,
        POSClosingEntryEntity::class,
        TaxDetailsEntity::class
    ], version = 4,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun posProfileDao(): POSProfileDao
    abstract fun paymentModesDao(): PaymentModesDao
    abstract fun cashboxDao(): CashboxDao
    abstract fun customerDao(): CustomerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleInvoiceDao(): SalesInvoiceDao
    abstract fun posOpeningDao(): POSOpeningEntryDao
    abstract fun posClosingDao(): POSClosingEntryDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect class DatabaseBuilder {
    fun build(): AppDatabase
}
