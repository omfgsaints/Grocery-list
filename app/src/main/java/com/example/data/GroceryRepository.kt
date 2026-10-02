package com.example.data

import com.example.data.dao.GroceryItemDao
import com.example.data.dao.PriceRecordDao
import com.example.data.dao.StoreDao
import com.example.data.entity.GroceryItem
import com.example.data.entity.PriceRecord
import com.example.data.entity.Store
import kotlinx.coroutines.flow.Flow

class GroceryRepository(
    private val groceryDao: GroceryItemDao,
    private val storeDao: StoreDao,
    private val priceRecordDao: PriceRecordDao
) {
    // Grocery items
    val allItems: Flow<List<GroceryItem>> = groceryDao.getAllItems()

    suspend fun insertItem(item: GroceryItem): Long {
        val id = groceryDao.insertItem(item)
        if (item.price > 0 && item.name.isNotBlank()) {
            recordPrice(
                productName = item.name.trim(),
                storeName = item.storeName.ifBlank { "General Store" },
                price = item.price,
                unit = item.unit,
                storeAddress = item.storeAddress,
                latitude = item.latitude,
                longitude = item.longitude
            )
        }
        return id
    }

    suspend fun updateItem(item: GroceryItem) {
        groceryDao.updateItem(item)
        if (item.price > 0 && item.name.isNotBlank()) {
            recordPrice(
                productName = item.name.trim(),
                storeName = item.storeName.ifBlank { "General Store" },
                price = item.price,
                unit = item.unit,
                storeAddress = item.storeAddress,
                latitude = item.latitude,
                longitude = item.longitude
            )
        }
    }

    suspend fun updateItemPrice(id: Long, newPrice: Double, item: GroceryItem) {
        groceryDao.updatePrice(id, newPrice)
        if (newPrice > 0 && item.name.isNotBlank()) {
            recordPrice(
                productName = item.name.trim(),
                storeName = item.storeName.ifBlank { "General Store" },
                price = newPrice,
                unit = item.unit,
                storeAddress = item.storeAddress,
                latitude = item.latitude,
                longitude = item.longitude
            )
        }
    }

    suspend fun updateItemQuantity(id: Long, quantity: Double) {
        if (quantity > 0) {
            groceryDao.updateQuantity(id, quantity)
        } else {
            groceryDao.deleteById(id)
        }
    }

    suspend fun toggleChecked(id: Long, isChecked: Boolean) {
        groceryDao.updateCheckedStatus(id, isChecked)
    }

    suspend fun deleteItem(item: GroceryItem) {
        groceryDao.deleteItem(item)
    }

    suspend fun deleteItemById(id: Long) {
        groceryDao.deleteById(id)
    }

    suspend fun clearChecked() {
        groceryDao.deleteCheckedItems()
    }

    suspend fun clearAll() {
        groceryDao.deleteAll()
    }

    suspend fun uncheckAll() {
        groceryDao.uncheckAll()
    }

    // Stores
    val allStores: Flow<List<Store>> = storeDao.getAllStores()

    suspend fun insertStore(store: Store): Long {
        return storeDao.insertStore(store)
    }

    suspend fun updateStore(store: Store) {
        storeDao.updateStore(store)
    }

    suspend fun deleteStore(store: Store) {
        storeDao.deleteStore(store)
    }

    suspend fun getStoreByName(name: String): Store? {
        return storeDao.getStoreByName(name)
    }

    // Price Records & Comparisons
    val allPriceRecords: Flow<List<PriceRecord>> = priceRecordDao.getAllRecords()
    val allProductNames: Flow<List<String>> = priceRecordDao.getAllUniqueProductNames()

    fun getRecordsForProduct(productName: String): Flow<List<PriceRecord>> {
        return priceRecordDao.getRecordsForProduct(productName.trim())
    }

    suspend fun recordPrice(
        productName: String,
        storeName: String,
        price: Double,
        unit: String,
        storeAddress: String = "",
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        val record = PriceRecord(
            productName = productName.trim(),
            storeName = storeName.trim(),
            price = price,
            unit = unit,
            storeAddress = storeAddress,
            latitude = latitude,
            longitude = longitude,
            recordedAt = System.currentTimeMillis()
        )
        return priceRecordDao.insertRecord(record)
    }

    suspend fun getLatestPriceForProductAtStore(productName: String, storeName: String): PriceRecord? {
        return priceRecordDao.getLatestPriceForProductAtStore(productName.trim(), storeName.trim())
    }

    suspend fun getLatestPriceForProduct(productName: String): PriceRecord? {
        return priceRecordDao.getLatestPriceForProduct(productName.trim())
    }

    suspend fun deleteRecordsByProductName(productName: String) {
        priceRecordDao.deleteRecordsByProductName(productName.trim())
    }
}
