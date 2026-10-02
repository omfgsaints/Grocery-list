package com.example.model

import com.example.data.entity.GroceryItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FamilySyncItem(
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val isChecked: Boolean,
    val storeName: String,
    val storeAddress: String,
    val notes: String
)

data class FamilySyncPayload(
    val syncCode: String,
    val groupName: String,
    val timestamp: Long,
    val items: List<FamilySyncItem>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("syncCode", syncCode)
        root.put("groupName", groupName)
        root.put("timestamp", timestamp)

        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("category", item.category)
            obj.put("quantity", item.quantity)
            obj.put("unit", item.unit)
            obj.put("price", item.price)
            obj.put("isChecked", item.isChecked)
            obj.put("storeName", item.storeName)
            obj.put("storeAddress", item.storeAddress)
            obj.put("notes", item.notes)
            array.put(obj)
        }
        root.put("items", array)
        return root.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): FamilySyncPayload? {
            return try {
                val root = JSONObject(jsonStr)
                val code = root.optString("syncCode", "FAM-CART")
                val group = root.optString("groupName", "Family Shopping List")
                val timestamp = root.optLong("timestamp", System.currentTimeMillis())
                val array = root.getJSONArray("items")

                val items = mutableListOf<FamilySyncItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    items.add(
                        FamilySyncItem(
                            name = obj.getString("name"),
                            category = obj.optString("category", "Pantry"),
                            quantity = obj.optDouble("quantity", 1.0),
                            unit = obj.optString("unit", "pcs"),
                            price = obj.optDouble("price", 0.0),
                            isChecked = obj.optBoolean("isChecked", false),
                            storeName = obj.optString("storeName", ""),
                            storeAddress = obj.optString("storeAddress", ""),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                FamilySyncPayload(code, group, timestamp, items)
            } catch (e: Exception) {
                null
            }
        }

        fun fromGroceryItems(
            items: List<GroceryItem>,
            groupName: String = "Tuazon Family Cart",
            code: String = "FAM-" + UUID.randomUUID().toString().take(4).uppercase()
        ): FamilySyncPayload {
            val syncItems = items.map {
                FamilySyncItem(
                    name = it.name,
                    category = it.category,
                    quantity = it.quantity,
                    unit = it.unit,
                    price = it.price,
                    isChecked = it.isChecked,
                    storeName = it.storeName,
                    storeAddress = it.storeAddress,
                    notes = it.notes
                )
            }
            return FamilySyncPayload(code, groupName, System.currentTimeMillis(), syncItems)
        }
    }
}
