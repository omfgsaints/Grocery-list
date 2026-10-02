package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiRecipeService
import com.example.data.AppDatabase
import com.example.data.GroceryRepository
import com.example.data.entity.GroceryItem
import com.example.data.entity.PriceRecord
import com.example.data.entity.Store
import com.example.location.LocationHelper
import com.example.location.LocationResult
import com.example.model.CurrencyConfig
import com.example.model.FamilySyncItem
import com.example.model.FamilySyncPayload
import com.example.model.RecipeIngredient
import com.example.model.RecipeResult
import com.example.model.SuggestedRecipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class GroceryTotals(
    val totalCost: Double = 0.0,
    val checkedCost: Double = 0.0,
    val remainingCost: Double = 0.0,
    val totalItemCount: Int = 0,
    val checkedItemCount: Int = 0,
    val budgetLimit: Double? = 2500.0,
    val isBudgetExceeded: Boolean = false,
    val budgetPercentage: Float = 0f,
    val currency: CurrencyConfig = CurrencyConfig.PHP
)

data class StorePriceComparison(
    val storeName: String,
    val storeAddress: String,
    val price: Double,
    val unit: String,
    val distanceMeters: Float?,
    val isBestPrice: Boolean,
    val isCurrentStore: Boolean
)

data class PriceEstimation(
    val productName: String,
    val lowestPrice: Double,
    val averagePrice: Double,
    val highestPrice: Double,
    val comparisons: List<StorePriceComparison>
)

data class ProductPriceSummary(
    val productName: String,
    val lowestPrice: Double,
    val lowestStoreName: String,
    val latestPrice: Double,
    val latestStoreName: String,
    val records: List<PriceRecord>
)

sealed interface RecipeSearchUiState {
    data object Idle : RecipeSearchUiState
    data object Loading : RecipeSearchUiState
    data class Success(val recipe: RecipeResult) : RecipeSearchUiState
    data class Error(val message: String) : RecipeSearchUiState
}

sealed interface RecipeSuggestUiState {
    data object Idle : RecipeSuggestUiState
    data object Loading : RecipeSuggestUiState
    data class Success(val recipes: List<SuggestedRecipe>) : RecipeSuggestUiState
    data class Error(val message: String) : RecipeSuggestUiState
}

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroceryRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = GroceryRepository(
            groceryDao = database.groceryItemDao(),
            storeDao = database.storeDao(),
            priceRecordDao = database.priceRecordDao()
        )
    }

    // Raw data flows
    val allItems: StateFlow<List<GroceryItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStores: StateFlow<List<Store>> = repository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPriceRecords: StateFlow<List<PriceRecord>> = repository.allPriceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currency Setting (Defaults to Philippine Peso PHP as requested)
    private val _currentCurrency = MutableStateFlow(CurrencyConfig.PHP)
    val currentCurrency: StateFlow<CurrencyConfig> = _currentCurrency.asStateFlow()

    // Budget Limiter (Defaults to ₱2,500.00)
    private val _budgetLimit = MutableStateFlow<Double?>(2500.0)
    val budgetLimit: StateFlow<Double?> = _budgetLimit.asStateFlow()

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStoreFilter = MutableStateFlow<String?>(null)
    val selectedStoreFilter: StateFlow<String?> = _selectedStoreFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Location state
    private val _currentLocation = MutableStateFlow<LocationResult?>(null)
    val currentLocation: StateFlow<LocationResult?> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _locationMessage = MutableStateFlow<String?>(null)
    val locationMessage: StateFlow<String?> = _locationMessage.asStateFlow()

    // AI Recipe States
    private val _recipeSearchState = MutableStateFlow<RecipeSearchUiState>(RecipeSearchUiState.Idle)
    val recipeSearchState: StateFlow<RecipeSearchUiState> = _recipeSearchState.asStateFlow()

    private val _recipeSuggestState = MutableStateFlow<RecipeSuggestUiState>(RecipeSuggestUiState.Idle)
    val recipeSuggestState: StateFlow<RecipeSuggestUiState> = _recipeSuggestState.asStateFlow()

    // Filtered Items
    val filteredItems: StateFlow<List<GroceryItem>> = combine(
        allItems,
        _searchQuery,
        _selectedStoreFilter,
        _selectedCategoryFilter
    ) { items, query, storeFilter, categoryFilter ->
        items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.storeName.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)

            val matchesStore = storeFilter == null || item.storeName.equals(storeFilter, ignoreCase = true)
            val matchesCategory = categoryFilter == null || item.category.equals(categoryFilter, ignoreCase = true)

            matchesQuery && matchesStore && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Totals & Budget calculations
    val totals: StateFlow<GroceryTotals> = combine(
        allItems,
        _budgetLimit,
        _currentCurrency
    ) { all, budget, currency ->
        var total = 0.0
        var checked = 0.0
        var totalCount = 0
        var checkedCount = 0

        for (item in all) {
            val cost = item.totalPrice
            total += cost
            totalCount += 1
            if (item.isChecked) {
                checked += cost
                checkedCount += 1
            }
        }

        val percentage = if (budget != null && budget > 0) {
            (total / budget).toFloat()
        } else 0f

        val exceeded = budget != null && total > budget

        GroceryTotals(
            totalCost = total,
            checkedCost = checked,
            remainingCost = (total - checked).coerceAtLeast(0.0),
            totalItemCount = totalCount,
            checkedItemCount = checkedCount,
            budgetLimit = budget,
            isBudgetExceeded = exceeded,
            budgetPercentage = percentage,
            currency = currency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroceryTotals())

    // Product price comparison summaries
    val productSummaries: StateFlow<List<ProductPriceSummary>> = allPriceRecords.combine(_searchQuery) { records, query ->
        val grouped = records.groupBy { it.productName.lowercase().trim() }
        grouped.map { (_, groupRecords) ->
            val sortedByPrice = groupRecords.sortedBy { it.price }
            val sortedByDate = groupRecords.sortedByDescending { it.recordedAt }
            val lowest = sortedByPrice.first()
            val latest = sortedByDate.first()
            ProductPriceSummary(
                productName = latest.productName,
                lowestPrice = lowest.price,
                lowestStoreName = lowest.storeName,
                latestPrice = latest.price,
                latestStoreName = latest.storeName,
                records = sortedByDate
            )
        }.filter { summary ->
            query.isBlank() || summary.productName.contains(query, ignoreCase = true)
        }.sortedBy { it.productName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrency(currency: CurrencyConfig) {
        _currentCurrency.value = currency
    }

    fun setBudgetLimit(limit: Double?) {
        _budgetLimit.value = limit
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStoreFilter(storeName: String?) {
        _selectedStoreFilter.value = storeName
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun clearLocationMessage() {
        _locationMessage.value = null
    }

    // Refresh current GPS location and reverse geocode with auto Philippine currency switch
    fun detectCurrentLocation() {
        val context = getApplication<Application>()
        if (!LocationHelper.hasLocationPermission(context)) {
            _locationMessage.value = "Location permission needed to tag store locations"
            return
        }

        viewModelScope.launch {
            _isLocating.value = true
            try {
                val loc = LocationHelper.fetchCurrentLocation(context)
                if (loc != null) {
                    val (address, feature) = LocationHelper.getAddressFromCoordinates(
                        context, loc.latitude, loc.longitude
                    )
                    val stores = allStores.value
                    val (nearestStore, dist) = LocationHelper.findNearestStore(
                        loc.latitude, loc.longitude, stores
                    )

                    // Auto-detect currency based on location
                    val detectedCurr = CurrencyConfig.detectCurrency(loc.latitude, loc.longitude, address)
                    if (detectedCurr != _currentCurrency.value) {
                        _currentCurrency.value = detectedCurr
                        _locationMessage.value = "Switched currency to ${detectedCurr.flag} ${detectedCurr.code} (${detectedCurr.symbol})"
                    }

                    val result = LocationResult(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = address,
                        suggestedStoreName = nearestStore?.name ?: feature.ifBlank { "Nearby Market" },
                        nearestSavedStore = nearestStore,
                        distanceToNearestStoreMeters = dist
                    )
                    _currentLocation.value = result
                    _locationMessage.value = if (nearestStore != null && (dist ?: 999f) < 200) {
                        "At ${nearestStore.name} (~${dist?.toInt()}m away) • ${detectedCurr.symbol}"
                    } else {
                        "Location: $address • ${detectedCurr.symbol}"
                    }
                } else {
                    _locationMessage.value = "Unable to determine current GPS location. Ensure GPS is on."
                }
            } catch (e: Exception) {
                _locationMessage.value = "Location error: ${e.localizedMessage}"
            } finally {
                _isLocating.value = false
            }
        }
    }

    // Fast price lookup for auto-suggesting prices
    suspend fun getPriceSuggestion(productName: String, storeName: String?): PriceRecord? {
        if (productName.isBlank()) return null
        if (!storeName.isNullOrBlank()) {
            val storePrice = repository.getLatestPriceForProductAtStore(productName, storeName)
            if (storePrice != null) return storePrice
        }
        return repository.getLatestPriceForProduct(productName)
    }

    // AI: Search Recipe Ingredients
    fun searchRecipeIngredients(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _recipeSearchState.value = RecipeSearchUiState.Loading
            try {
                val symbol = _currentCurrency.value.symbol
                val result = GeminiRecipeService.searchRecipeIngredients(query, symbol)
                _recipeSearchState.value = RecipeSearchUiState.Success(result)
            } catch (e: Exception) {
                _recipeSearchState.value = RecipeSearchUiState.Error(e.localizedMessage ?: "Failed to find recipe")
            }
        }
    }

    // AI: Add Selected Recipe Ingredients to Grocery List
    fun addRecipeIngredientsToList(ingredients: List<RecipeIngredient>, targetStore: String = "") {
        viewModelScope.launch {
            ingredients.filter { it.isSelected }.forEach { ing ->
                addItem(
                    name = ing.name,
                    category = ing.category,
                    quantity = ing.quantity,
                    unit = ing.unit,
                    price = ing.estimatedPrice,
                    storeName = targetStore.ifBlank { _selectedStoreFilter.value ?: "Supermarket" },
                    storeAddress = "",
                    latitude = null,
                    longitude = null,
                    notes = "Recipe ingredient"
                )
            }
            _locationMessage.value = "Added ${ingredients.count { it.isSelected }} ingredients to list!"
        }
    }

    // AI: Suggest Recipes from Currently Bought / In-Cart Items
    fun loadRecipeSuggestionsForBoughtItems() {
        viewModelScope.launch {
            _recipeSuggestState.value = RecipeSuggestUiState.Loading
            try {
                // Get checked (in cart) items or all current items
                val items = allItems.value
                val boughtNames = items.filter { it.isChecked }.map { it.name }
                    .ifEmpty { items.map { it.name } }

                val symbol = _currentCurrency.value.symbol
                val suggestions = GeminiRecipeService.suggestRecipesFromIngredients(boughtNames, symbol)
                _recipeSuggestState.value = RecipeSuggestUiState.Success(suggestions)
            } catch (e: Exception) {
                _recipeSuggestState.value = RecipeSuggestUiState.Error(e.localizedMessage ?: "Failed to generate suggestions")
            }
        }
    }

    // Family Sync & Online Sharing Operations
    fun exportFamilySyncPayload(groupName: String = "Family Shopping List"): String {
        val payload = FamilySyncPayload.fromGroceryItems(allItems.value, groupName)
        return payload.toJsonString()
    }

    fun importFamilySyncPayload(jsonString: String, merge: Boolean = true): Boolean {
        val payload = FamilySyncPayload.fromJsonString(jsonString) ?: return false
        viewModelScope.launch {
            if (!merge) {
                repository.clearAll()
            }
            payload.items.forEach { item ->
                addItem(
                    name = item.name,
                    category = item.category,
                    quantity = item.quantity,
                    unit = item.unit,
                    price = item.price,
                    storeName = item.storeName,
                    storeAddress = item.storeAddress,
                    latitude = null,
                    longitude = null,
                    notes = item.notes
                )
            }
            _locationMessage.value = "Successfully imported ${payload.items.size} items from ${payload.groupName}!"
        }
        return true
    }

    fun exportCartAsShareableText(): String {
        val sb = StringBuilder()
        val curr = _currentCurrency.value
        sb.append("🛒 SmartCart - ${curr.flag} Grocery List\n")
        sb.append("-----------------------------\n")
        allItems.value.forEach { item ->
            val status = if (item.isChecked) "✅" else "⬜"
            sb.append("$status ${item.name} (${item.quantity} ${item.unit})")
            if (item.price > 0) {
                sb.append(" - ${curr.symbol}${String.format(Locale.US, "%.2f", item.totalPrice)}")
            }
            if (item.storeName.isNotBlank()) {
                sb.append(" @ ${item.storeName}")
            }
            sb.append("\n")
        }
        val t = totals.value
        sb.append("-----------------------------\n")
        sb.append("Total: ${curr.symbol}${String.format(Locale.US, "%.2f", t.totalCost)}")
        if (t.budgetLimit != null) {
            sb.append(" / Budget: ${curr.symbol}${String.format(Locale.US, "%.2f", t.budgetLimit)}")
        }
        sb.append("\n\nShared via SmartCart App")
        return sb.toString()
    }

    // Item Actions
    fun addItem(
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        price: Double,
        storeName: String,
        storeAddress: String,
        latitude: Double?,
        longitude: Double?,
        notes: String
    ) {
        viewModelScope.launch {
            var storeId: Long? = null
            if (storeName.isNotBlank()) {
                val existingStore = repository.getStoreByName(storeName)
                if (existingStore != null) {
                    storeId = existingStore.id
                    if (latitude != null && longitude != null && existingStore.latitude == 0.0) {
                        repository.updateStore(
                            existingStore.copy(
                                latitude = latitude,
                                longitude = longitude,
                                address = storeAddress.ifBlank { existingStore.address },
                                lastVisitedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else if (latitude != null && longitude != null) {
                    storeId = repository.insertStore(
                        Store(
                            name = storeName,
                            address = storeAddress,
                            latitude = latitude,
                            longitude = longitude,
                            iconEmoji = getEmojiForCategory(category)
                        )
                    )
                }
            }

            val item = GroceryItem(
                name = name.trim(),
                category = category,
                quantity = quantity.coerceAtLeast(0.1),
                unit = unit,
                price = price.coerceAtLeast(0.0),
                isChecked = false,
                storeId = storeId,
                storeName = storeName.trim(),
                storeAddress = storeAddress.trim(),
                latitude = latitude,
                longitude = longitude,
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertItem(item)
        }
    }

    fun updateItem(item: GroceryItem) {
        viewModelScope.launch {
            repository.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateItemPrice(item: GroceryItem, newPrice: Double, storeName: String? = null, storeAddress: String? = null) {
        viewModelScope.launch {
            val updated = item.copy(
                price = newPrice.coerceAtLeast(0.0),
                storeName = storeName ?: item.storeName,
                storeAddress = storeAddress ?: item.storeAddress,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateItem(updated)
        }
    }

    fun updateQuantity(id: Long, quantity: Double) {
        viewModelScope.launch {
            repository.updateItemQuantity(id, quantity)
        }
    }

    fun toggleItemChecked(item: GroceryItem) {
        viewModelScope.launch {
            repository.toggleChecked(item.id, !item.isChecked)
        }
    }

    fun deleteItem(item: GroceryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun clearCheckedItems() {
        viewModelScope.launch {
            repository.clearChecked()
        }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun uncheckAllItems() {
        viewModelScope.launch {
            repository.uncheckAll()
        }
    }

    fun addProductToList(summary: ProductPriceSummary, targetStoreName: String? = null) {
        val selectedRecord = if (targetStoreName != null) {
            summary.records.firstOrNull { it.storeName.equals(targetStoreName, ignoreCase = true) }
                ?: summary.records.first()
        } else {
            summary.records.minByOrNull { it.price } ?: summary.records.first()
        }

        addItem(
            name = summary.productName,
            category = "Pantry",
            quantity = 1.0,
            unit = selectedRecord.unit,
            price = selectedRecord.price,
            storeName = selectedRecord.storeName,
            storeAddress = selectedRecord.storeAddress,
            latitude = selectedRecord.latitude,
            longitude = selectedRecord.longitude,
            notes = "Re-added from price catalog"
        )
    }

    fun addOrUpdateStore(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        emoji: String = "🛒"
    ) {
        viewModelScope.launch {
            val existing = repository.getStoreByName(name)
            if (existing != null) {
                repository.updateStore(
                    existing.copy(
                        address = address.ifBlank { existing.address },
                        latitude = if (latitude != 0.0) latitude else existing.latitude,
                        longitude = if (longitude != 0.0) longitude else existing.longitude,
                        iconEmoji = emoji,
                        lastVisitedAt = System.currentTimeMillis()
                    )
                )
            } else {
                repository.insertStore(
                    Store(
                        name = name.trim(),
                        address = address.trim(),
                        latitude = latitude,
                        longitude = longitude,
                        iconEmoji = emoji,
                        lastVisitedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteStore(store: Store) {
        viewModelScope.launch {
            repository.deleteStore(store)
        }
    }

    // Price comparison across stores for a specific item
    fun getNearbyStoreComparisons(itemName: String, currentStoreName: String): List<StorePriceComparison> {
        val records = allPriceRecords.value
        val stores = allStores.value
        val userLoc = _currentLocation.value

        val cleanItem = itemName.trim().lowercase()
        // Match items with overlapping name or keywords
        val matchedRecords = records.filter { rec ->
            val cleanRec = rec.productName.trim().lowercase()
            cleanRec.contains(cleanItem) || cleanItem.contains(cleanRec) ||
                    cleanItem.split(" ").any { word -> word.length > 3 && cleanRec.contains(word) }
        }

        if (matchedRecords.isEmpty()) return emptyList()

        // Group by store name and get lowest/latest price per store
        val byStore = matchedRecords.groupBy { it.storeName.lowercase() }
        val lowestOverall = matchedRecords.minOfOrNull { it.price } ?: 0.0

        return byStore.map { (_, storeRecs) ->
            val latest = storeRecs.maxByOrNull { it.recordedAt }!!
            val storeObj = stores.firstOrNull { it.name.equals(latest.storeName, ignoreCase = true) }

            val distance = if (userLoc != null && storeObj != null && storeObj.latitude != 0.0) {
                LocationHelper.distanceBetweenMeters(
                    userLoc.latitude, userLoc.longitude,
                    storeObj.latitude, storeObj.longitude
                )
            } else null

            StorePriceComparison(
                storeName = latest.storeName,
                storeAddress = latest.storeAddress.ifBlank { storeObj?.address ?: "" },
                price = latest.price,
                unit = latest.unit,
                distanceMeters = distance,
                isBestPrice = latest.price == lowestOverall,
                isCurrentStore = latest.storeName.equals(currentStoreName, ignoreCase = true)
            )
        }.sortedBy { it.price }
    }

    // Price search and estimation
    fun getEstimatedPricing(query: String): PriceEstimation? {
        if (query.isBlank()) return null
        val comparisons = getNearbyStoreComparisons(query, "")
        if (comparisons.isEmpty()) return null

        val prices = comparisons.map { it.price }
        return PriceEstimation(
            productName = query.trim(),
            lowestPrice = prices.minOrNull() ?: 0.0,
            averagePrice = prices.average(),
            highestPrice = prices.maxOrNull() ?: 0.0,
            comparisons = comparisons
        )
    }

    // Auto-Add Recipe Ingredients directly from dish name
    fun autoAddRecipe(recipeName: String) {
        viewModelScope.launch {
            val symbol = _currentCurrency.value.symbol
            val recipe = GeminiRecipeService.searchRecipeIngredients(recipeName, symbol)
            addRecipeIngredientsToList(recipe.ingredients, _selectedStoreFilter.value ?: "")
        }
    }

    private fun getEmojiForCategory(category: String): String {
        return when (category.lowercase()) {
            "produce" -> "🥬"
            "dairy" -> "🧀"
            "bakery" -> "🍞"
            "meat & seafood", "meat" -> "🥩"
            "pantry" -> "🥫"
            "frozen" -> "🍦"
            "beverages", "drinks" -> "🧃"
            "snacks" -> "🥨"
            "household" -> "🧼"
            else -> "🛒"
        }
    }
}
