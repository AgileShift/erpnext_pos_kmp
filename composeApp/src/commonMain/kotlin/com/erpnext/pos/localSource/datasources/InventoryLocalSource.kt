package com.erpnext.pos.localSource.datasources

import androidx.paging.PagingSource
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

interface IInventoryLocalSource {
    suspend fun insertAll(inventory: List<ItemEntity>)
    fun getAllPaged(): PagingSource<Int, ItemEntity>
    fun getAll(): List<ItemEntity>
    fun getItemById(id: String): PagingSource<Int, ItemEntity>
    fun getAllFilteredPaged(search: String): PagingSource<Int, ItemEntity>
    fun getItemCategories(): Flow<List<CategoryEntity>>
    fun deleteAllCategories()
    fun insertCategories(data: List<CategoryEntity>)
    suspend fun getOldestItem(): ItemEntity?
    suspend fun count(): Int
    suspend fun deleteAll()
}

class InventoryLocalSource(private val dao: ItemDao, private val categoryDao: CategoryDao) :
    IInventoryLocalSource {
    override suspend fun insertAll(inventory: List<ItemEntity>) = dao.addItems(inventory)

    override fun getAllPaged(): PagingSource<Int, ItemEntity> = dao.getAllItemsPaged()
    override fun getAll(): List<ItemEntity> = dao.getAllItems()

    override fun getItemById(id: String): PagingSource<Int, ItemEntity> = dao.getItemById(id)

    fun getAllFiltered(search: String): Flow<List<ItemEntity>> =
        dao.getAllItemsFiltered(search)

    override fun getAllFilteredPaged(search: String): PagingSource<Int, ItemEntity> =
        dao.getAllFiltered(search)

    override suspend fun deleteAll() = dao.deleteAll()

    override fun deleteAllCategories() = categoryDao.deleteAll()
    override fun insertCategories(data: List<CategoryEntity>) = categoryDao.insertAll(data)
    override fun getItemCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()
    override suspend fun count(): Int = dao.countAll()
    override suspend fun getOldestItem(): ItemEntity? = dao.getOldestItem()
}