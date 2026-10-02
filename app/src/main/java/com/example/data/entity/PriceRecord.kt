package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_records")
data class PriceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val storeName: String,
    val price: Double,
    val unit: String = "pcs",
    val storeAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recordedAt: Long = System.currentTimeMillis()
)
