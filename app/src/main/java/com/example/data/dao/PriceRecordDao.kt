package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.PriceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceRecordDao {
    @Query("SELECT * FROM price_records ORDER BY recordedAt DESC")
    fun getAllRecords(): Flow<List<PriceRecord>>

    @Query("SELECT * FROM price_records WHERE LOWER(productName) = LOWER(:productName) ORDER BY recordedAt DESC")
    fun getRecordsForProduct(productName: String): Flow<List<PriceRecord>>

    @Query("SELECT * FROM price_records WHERE LOWER(productName) = LOWER(:productName) AND LOWER(storeName) = LOWER(:storeName) ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestPriceForProductAtStore(productName: String, storeName: String): PriceRecord?

    @Query("SELECT * FROM price_records WHERE LOWER(productName) = LOWER(:productName) ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestPriceForProduct(productName: String): PriceRecord?

    @Query("SELECT DISTINCT productName FROM price_records ORDER BY productName ASC")
    fun getAllUniqueProductNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PriceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PriceRecord>)

    @Delete
    suspend fun deleteRecord(record: PriceRecord)

    @Query("DELETE FROM price_records WHERE LOWER(productName) = LOWER(:productName)")
    suspend fun deleteRecordsByProductName(productName: String)
}
