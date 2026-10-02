package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GroceryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryItemDao {
    @Query("SELECT * FROM grocery_items ORDER BY isChecked ASC, id DESC")
    fun getAllItems(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE id = :id")
    suspend fun getItemById(id: Long): GroceryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GroceryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GroceryItem>)

    @Update
    suspend fun updateItem(item: GroceryItem)

    @Delete
    suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE grocery_items SET isChecked = :isChecked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCheckedStatus(id: Long, isChecked: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE grocery_items SET price = :price, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePrice(id: Long, price: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE grocery_items SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM grocery_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems()

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll()

    @Query("UPDATE grocery_items SET isChecked = 0")
    suspend fun uncheckAll()
}
