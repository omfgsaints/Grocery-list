package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.GroceryItem
import com.example.data.entity.PriceRecord
import com.example.model.CurrencyConfig
import com.example.model.FamilySyncItem
import com.example.model.FamilySyncPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SmartCart", appName)
    }

    @Test
    fun `grocery item total calculation`() {
        val item = GroceryItem(
            name = "Organic Bananas",
            quantity = 3.5,
            price = 0.60
        )
        assertEquals(2.10, item.totalPrice, 0.001)
    }

    @Test
    fun `price record data model validation`() {
        val record = PriceRecord(
            productName = "Whole Milk",
            storeName = "Trader Joe's",
            price = 3.99,
            unit = "gal"
        )
        assertEquals("Whole Milk", record.productName)
        assertEquals("Trader Joe's", record.storeName)
        assertEquals(3.99, record.price, 0.001)
    }

    @Test
    fun `philippine location currency detection`() {
        // Manila coordinates: 14.5995° N, 120.9842° E
        val detected = CurrencyConfig.detectCurrency(14.5995, 120.9842, "Makati City, Metro Manila, Philippines")
        assertEquals(CurrencyConfig.PHP, detected)
        assertEquals("₱", detected.symbol)
        assertEquals("PHP", detected.code)
    }

    @Test
    fun `family sync payload json roundtrip`() {
        val payload = FamilySyncPayload(
            syncCode = "FAM-1234",
            groupName = "Cruz Family",
            timestamp = 1700000000L,
            items = listOf(
                FamilySyncItem(
                    name = "Chicken Thighs",
                    category = "Meat",
                    quantity = 2.0,
                    unit = "kg",
                    price = 240.0,
                    isChecked = false,
                    storeName = "SM Supermarket",
                    storeAddress = "Megamall, Mandaluyong",
                    notes = "For Adobo"
                )
            )
        )

        val json = payload.toJsonString()
        val parsed = FamilySyncPayload.fromJsonString(json)
        assertNotNull(parsed)
        assertEquals("FAM-1234", parsed!!.syncCode)
        assertEquals("Cruz Family", parsed.groupName)
        assertEquals(1, parsed.items.size)
        assertEquals("Chicken Thighs", parsed.items[0].name)
        assertEquals(240.0, parsed.items[0].price, 0.001)
    }
}
