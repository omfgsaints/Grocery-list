package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Produce",
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val price: Double = 0.0,
    val isChecked: Boolean = false,
    val storeId: Long? = null,
    val storeName: String = "",
    val storeAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalPrice: Double
        get() = quantity * price
}
