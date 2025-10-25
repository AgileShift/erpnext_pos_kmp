package com.erpnext.pos.localSource.datasources

import androidx.paging.PagingSource
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

interface IInventoryLocalSource {
    suspend fun insertAll(inventory: List<ItemEntity>)
    fun getAll(): PagingSource<Int, ItemEntity>
    fun getItemById(id: String): PagingSource<Int, ItemEntity>
    fun getAllFiltered(search: String): PagingSource<Int, ItemEntity>
    fun getItemCategories(): Flow<List<CategoryEntity>>
    fun deleteAllCategories()
    fun insertCategories(data: List<CategoryEntity>)
    suspend fun count(): Int
}

class InventoryLocalSource(private val dao: ItemDao, private val categoryDao: CategoryDao) :
    IInventoryLocalSource {
    override suspend fun insertAll(inventory: List<ItemEntity>) = dao.addItems(inventory)

    override fun getAll(): PagingSource<Int, ItemEntity> = dao.getAllItems()

    override fun getItemById(id: String): PagingSource<Int, ItemEntity> = dao.getItemById(id)

    override fun getAllFiltered(search: String): PagingSource<Int, ItemEntity> =
        dao.getAllFiltered(search)

    override fun deleteAllCategories() = categoryDao.deleteAll()
    override fun insertCategories(data: List<CategoryEntity>) = categoryDao.insertAll(data)
    override fun getItemCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    override suspend fun count(): Int = dao.countAll()
}