package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.GroceryItemDao
import com.example.data.dao.PriceRecordDao
import com.example.data.dao.StoreDao
import com.example.data.entity.GroceryItem
import com.example.data.entity.PriceRecord
import com.example.data.entity.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GroceryItem::class, Store::class, PriceRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groceryItemDao(): GroceryItemDao
    abstract fun storeDao(): StoreDao
    abstract fun priceRecordDao(): PriceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_cart_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(
                            database.storeDao(),
                            database.groceryItemDao(),
                            database.priceRecordDao()
                        )
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            storeDao: StoreDao,
            groceryDao: GroceryItemDao,
            priceDao: PriceRecordDao
        ) {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            val initialStores = listOf(
                Store(
                    id = 1,
                    name = "Puregold Price Club",
                    address = "E. Rodriguez Sr. Ave, Quezon City, Metro Manila",
                    latitude = 14.6225,
                    longitude = 121.0255,
                    iconEmoji = "🛒",
                    lastVisitedAt = now - dayMs
                ),
                Store(
                    id = 2,
                    name = "SM Supermarket",
                    address = "EDSA Cor. Doña Julia Vargas Ave, Mandaluyong, Metro Manila",
                    latitude = 14.5842,
                    longitude = 121.0567,
                    iconEmoji = "🏪",
                    lastVisitedAt = now - 2 * dayMs
                ),
                Store(
                    id = 3,
                    name = "Robinsons Supermarket",
                    address = "Robinsons Galleria, Ortigas Ave, Quezon City",
                    latitude = 14.5901,
                    longitude = 121.0602,
                    iconEmoji = "🍎",
                    lastVisitedAt = now - 3 * dayMs
                ),
                Store(
                    id = 4,
                    name = "Trader Joe's",
                    address = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    iconEmoji = "🌿",
                    lastVisitedAt = now - 4 * dayMs
                ),
                Store(
                    id = 5,
                    name = "Costco Wholesale",
                    address = "1000 Charleston Rd, Mountain View, CA",
                    latitude = 37.4180,
                    longitude = -122.0950,
                    iconEmoji = "📦",
                    lastVisitedAt = now - 6 * dayMs
                )
            )
            storeDao.insertAll(initialStores)

            val initialPriceRecords = listOf(
                // Chicken (Adobo Cut)
                PriceRecord(
                    productName = "Fresh Chicken (Adobo Cut)",
                    storeName = "Puregold Price Club",
                    price = 210.0,
                    unit = "kg",
                    storeAddress = "E. Rodriguez Sr. Ave, Quezon City",
                    latitude = 14.6225,
                    longitude = 121.0255,
                    recordedAt = now - dayMs
                ),
                PriceRecord(
                    productName = "Fresh Chicken (Adobo Cut)",
                    storeName = "SM Supermarket",
                    price = 235.0,
                    unit = "kg",
                    storeAddress = "EDSA, Mandaluyong",
                    latitude = 14.5842,
                    longitude = 121.0567,
                    recordedAt = now - 2 * dayMs
                ),
                PriceRecord(
                    productName = "Fresh Chicken (Adobo Cut)",
                    storeName = "Robinsons Supermarket",
                    price = 225.0,
                    unit = "kg",
                    storeAddress = "Ortigas Ave, Quezon City",
                    latitude = 14.5901,
                    longitude = 121.0602,
                    recordedAt = now - 4 * dayMs
                ),
                // Pork Belly Liempo
                PriceRecord(
                    productName = "Pork Belly (Liempo)",
                    storeName = "Puregold Price Club",
                    price = 320.0,
                    unit = "kg",
                    storeAddress = "E. Rodriguez Sr. Ave, Quezon City",
                    latitude = 14.6225,
                    longitude = 121.0255,
                    recordedAt = now - dayMs
                ),
                PriceRecord(
                    productName = "Pork Belly (Liempo)",
                    storeName = "SM Supermarket",
                    price = 350.0,
                    unit = "kg",
                    storeAddress = "EDSA, Mandaluyong",
                    latitude = 14.5842,
                    longitude = 121.0567,
                    recordedAt = now - 3 * dayMs
                ),
                // Fresh Eggs
                PriceRecord(
                    productName = "Farm Fresh Eggs (Dozen)",
                    storeName = "Puregold Price Club",
                    price = 114.0,
                    unit = "dozen",
                    storeAddress = "E. Rodriguez Sr. Ave, Quezon City",
                    latitude = 14.6225,
                    longitude = 121.0255,
                    recordedAt = now - dayMs
                ),
                PriceRecord(
                    productName = "Farm Fresh Eggs (Dozen)",
                    storeName = "SM Supermarket",
                    price = 126.0,
                    unit = "dozen",
                    storeAddress = "EDSA, Mandaluyong",
                    latitude = 14.5842,
                    longitude = 121.0567,
                    recordedAt = now - 2 * dayMs
                ),
                // Bananas
                PriceRecord(
                    productName = "Lakatan Bananas",
                    storeName = "Puregold Price Club",
                    price = 85.0,
                    unit = "kg",
                    storeAddress = "E. Rodriguez Sr. Ave, Quezon City",
                    latitude = 14.6225,
                    longitude = 121.0255,
                    recordedAt = now - 2 * dayMs
                ),
                PriceRecord(
                    productName = "Lakatan Bananas",
                    storeName = "Robinsons Supermarket",
                    price = 90.0,
                    unit = "kg",
                    storeAddress = "Ortigas Ave, Quezon City",
                    latitude = 14.5901,
                    longitude = 121.0602,
                    recordedAt = now - 5 * dayMs
                ),
                // Whole Milk
                PriceRecord(
                    productName = "Whole Organic Milk",
                    storeName = "Trader Joe's",
                    price = 3.99,
                    unit = "gal",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    recordedAt = now - 3 * dayMs
                ),
                PriceRecord(
                    productName = "Whole Organic Milk",
                    storeName = "Safeway Supermarket",
                    price = 4.49,
                    unit = "gal",
                    storeAddress = "2811 Middlefield Rd, Palo Alto, CA",
                    latitude = 37.4385,
                    longitude = -122.1245,
                    recordedAt = now - 7 * dayMs
                ),
                PriceRecord(
                    productName = "Whole Organic Milk",
                    storeName = "Costco Wholesale",
                    price = 3.49,
                    unit = "gal",
                    storeAddress = "1000 Charleston Rd, Mountain View, CA",
                    latitude = 37.4180,
                    longitude = -122.0950,
                    recordedAt = now - 10 * dayMs
                ),
                // Bananas
                PriceRecord(
                    productName = "Organic Bananas",
                    storeName = "Trader Joe's",
                    price = 0.25,
                    unit = "ea",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    recordedAt = now - 2 * dayMs
                ),
                PriceRecord(
                    productName = "Organic Bananas",
                    storeName = "Safeway Supermarket",
                    price = 0.69,
                    unit = "lb",
                    storeAddress = "2811 Middlefield Rd, Palo Alto, CA",
                    latitude = 37.4385,
                    longitude = -122.1245,
                    recordedAt = now - 5 * dayMs
                ),
                // Eggs
                PriceRecord(
                    productName = "Pasture-Raised Eggs (Dozen)",
                    storeName = "Trader Joe's",
                    price = 4.49,
                    unit = "dozen",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    recordedAt = now - 1 * dayMs
                ),
                PriceRecord(
                    productName = "Pasture-Raised Eggs (Dozen)",
                    storeName = "Whole Foods Market",
                    price = 5.99,
                    unit = "dozen",
                    storeAddress = "774 Emerson St, Palo Alto, CA",
                    latitude = 37.4445,
                    longitude = -122.1601,
                    recordedAt = now - 6 * dayMs
                ),
                // Sourdough Bread
                PriceRecord(
                    productName = "Artisan Sourdough Bread",
                    storeName = "Trader Joe's",
                    price = 3.29,
                    unit = "loaf",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    recordedAt = now - 2 * dayMs
                ),
                // Avocados
                PriceRecord(
                    productName = "Hass Avocados (Bag)",
                    storeName = "Costco Wholesale",
                    price = 5.99,
                    unit = "bag (6ct)",
                    storeAddress = "1000 Charleston Rd, Mountain View, CA",
                    latitude = 37.4180,
                    longitude = -122.0950,
                    recordedAt = now - 4 * dayMs
                ),
                PriceRecord(
                    productName = "Hass Avocados (Bag)",
                    storeName = "Trader Joe's",
                    price = 4.49,
                    unit = "bag (4ct)",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    recordedAt = now - 8 * dayMs
                )
            )
            priceDao.insertAll(initialPriceRecords)

            val initialGroceryItems = listOf(
                GroceryItem(
                    name = "Whole Organic Milk",
                    category = "Dairy",
                    quantity = 2.0,
                    unit = "gal",
                    price = 3.99,
                    isChecked = false,
                    storeId = 1,
                    storeName = "Trader Joe's",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    notes = "Check expiry date",
                    updatedAt = now
                ),
                GroceryItem(
                    name = "Organic Bananas",
                    category = "Produce",
                    quantity = 6.0,
                    unit = "ea",
                    price = 0.25,
                    isChecked = true,
                    storeId = 1,
                    storeName = "Trader Joe's",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    notes = "Slightly green",
                    updatedAt = now
                ),
                GroceryItem(
                    name = "Pasture-Raised Eggs (Dozen)",
                    category = "Dairy",
                    quantity = 1.0,
                    unit = "dozen",
                    price = 4.49,
                    isChecked = false,
                    storeId = 1,
                    storeName = "Trader Joe's",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    notes = "Vital Farms or Trader Joe's brand",
                    updatedAt = now
                ),
                GroceryItem(
                    name = "Hass Avocados (Bag)",
                    category = "Produce",
                    quantity = 1.0,
                    unit = "bag (6ct)",
                    price = 5.99,
                    isChecked = false,
                    storeId = 3,
                    storeName = "Costco Wholesale",
                    storeAddress = "1000 Charleston Rd, Mountain View, CA",
                    latitude = 37.4180,
                    longitude = -122.0950,
                    notes = "For guacamole & breakfast toast",
                    updatedAt = now
                ),
                GroceryItem(
                    name = "Artisan Sourdough Bread",
                    category = "Bakery",
                    quantity = 1.0,
                    unit = "loaf",
                    price = 3.29,
                    isChecked = false,
                    storeId = 1,
                    storeName = "Trader Joe's",
                    storeAddress = "855 El Camino Real, Palo Alto, CA",
                    latitude = 37.4429,
                    longitude = -122.1614,
                    notes = "Fresh baked",
                    updatedAt = now
                )
            )
            groceryDao.insertAll(initialGroceryItems)
        }
    }
}
