package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.GroceryItem
import com.example.ui.components.AddEditItemDialog
import com.example.ui.components.AddStoreDialog
import com.example.ui.components.BudgetDialog
import com.example.ui.components.CurrencyDialog
import com.example.ui.components.FamilySyncDialog
import com.example.ui.components.QuickPriceUpdateDialog
import com.example.ui.screens.GroceryListScreen
import com.example.ui.screens.PriceCatalogScreen
import com.example.ui.screens.RecipeAiScreen
import com.example.ui.screens.StoresScreen

enum class MainTab(val title: String) {
    GROCERY_LIST("List"),
    AI_RECIPES("Recipes"),
    PRICE_CATALOG("Prices"),
    STORES("Stores")
}

@Composable
fun MainScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.GROCERY_LIST) }

    // Dialog state
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<GroceryItem?>(null) }
    var itemToUpdatePrice by remember { mutableStateOf<GroceryItem?>(null) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showFamilySyncDialog by remember { mutableStateOf(false) }

    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val stores by viewModel.allStores.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val currency by viewModel.currentCurrency.collectAsStateWithLifecycle()
    val budgetLimit by viewModel.budgetLimit.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                // Tab 1: Grocery List
                NavigationBarItem(
                    selected = selectedTab == MainTab.GROCERY_LIST,
                    onClick = { selectedTab = MainTab.GROCERY_LIST },
                    icon = {
                        val remaining = totals.totalItemCount - totals.checkedItemCount
                        if (remaining > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("$remaining")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Grocery List")
                            }
                        } else {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Grocery List")
                        }
                    },
                    label = { Text("List", fontWeight = if (selectedTab == MainTab.GROCERY_LIST) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_grocery")
                )

                // Tab 2: AI Recipes
                NavigationBarItem(
                    selected = selectedTab == MainTab.AI_RECIPES,
                    onClick = { selectedTab = MainTab.AI_RECIPES },
                    icon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Recipes")
                    },
                    label = { Text("AI Recipes", fontWeight = if (selectedTab == MainTab.AI_RECIPES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_recipes")
                )

                // Tab 3: Price Catalog
                NavigationBarItem(
                    selected = selectedTab == MainTab.PRICE_CATALOG,
                    onClick = { selectedTab = MainTab.PRICE_CATALOG },
                    icon = {
                        Icon(Icons.Default.Sell, contentDescription = "Price Catalog")
                    },
                    label = { Text("Prices", fontWeight = if (selectedTab == MainTab.PRICE_CATALOG) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_catalog")
                )

                // Tab 4: Stores & Locations
                NavigationBarItem(
                    selected = selectedTab == MainTab.STORES,
                    onClick = { selectedTab = MainTab.STORES },
                    icon = {
                        Icon(Icons.Default.LocationOn, contentDescription = "Stores & GPS")
                    },
                    label = { Text("Stores", fontWeight = if (selectedTab == MainTab.STORES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_stores")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            label = "tab_switch_animation",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                MainTab.GROCERY_LIST -> {
                    GroceryListScreen(
                        viewModel = viewModel,
                        onAddItemClick = {
                            itemToEdit = null
                            showAddItemDialog = true
                        },
                        onEditItemClick = { item ->
                            itemToEdit = item
                            showAddItemDialog = true
                        },
                        onUpdatePriceClick = { item ->
                            itemToUpdatePrice = item
                        },
                        onOpenBudgetDialog = { showBudgetDialog = true },
                        onOpenCurrencyDialog = { showCurrencyDialog = true },
                        onOpenFamilySyncDialog = { showFamilySyncDialog = true }
                    )
                }
                MainTab.AI_RECIPES -> {
                    RecipeAiScreen(
                        viewModel = viewModel
                    )
                }
                MainTab.PRICE_CATALOG -> {
                    PriceCatalogScreen(
                        viewModel = viewModel,
                        onLogNewPriceClick = {
                            itemToEdit = null
                            showAddItemDialog = true
                        }
                    )
                }
                MainTab.STORES -> {
                    StoresScreen(
                        viewModel = viewModel,
                        onAddStoreClick = {
                            showAddStoreDialog = true
                        },
                        onFilterByStore = { storeName ->
                            viewModel.setStoreFilter(storeName)
                            selectedTab = MainTab.GROCERY_LIST
                        }
                    )
                }
            }
        }

        // Add / Edit Item Dialog
        if (showAddItemDialog) {
            AddEditItemDialog(
                itemToEdit = itemToEdit,
                stores = stores,
                currentLocation = currentLocation,
                currencySymbol = totals.currency.symbol,
                isLocating = isLocating,
                onDetectLocation = { viewModel.detectCurrentLocation() },
                onPriceLookup = { prodName, storeName ->
                    viewModel.getPriceSuggestion(prodName, storeName)
                },
                onDismiss = {
                    showAddItemDialog = false
                    itemToEdit = null
                },
                onConfirm = { name, category, quantity, unit, price, storeName, storeAddress, lat, lng, notes ->
                    if (itemToEdit == null) {
                        viewModel.addItem(
                            name = name,
                            category = category,
                            quantity = quantity,
                            unit = unit,
                            price = price,
                            storeName = storeName,
                            storeAddress = storeAddress,
                            latitude = lat,
                            longitude = lng,
                            notes = notes
                        )
                    } else {
                        viewModel.updateItem(
                            itemToEdit!!.copy(
                                name = name,
                                category = category,
                                quantity = quantity,
                                unit = unit,
                                price = price,
                                storeName = storeName,
                                storeAddress = storeAddress,
                                latitude = lat,
                                longitude = lng,
                                notes = notes
                            )
                        )
                    }
                    showAddItemDialog = false
                    itemToEdit = null
                }
            )
        }

        // Quick Price Update Dialog
        itemToUpdatePrice?.let { item ->
            QuickPriceUpdateDialog(
                item = item,
                currentLocation = currentLocation,
                currencySymbol = totals.currency.symbol,
                onDismiss = { itemToUpdatePrice = null },
                onConfirm = { newPrice, storeName, storeAddress ->
                    viewModel.updateItemPrice(
                        item = item,
                        newPrice = newPrice,
                        storeName = storeName.ifBlank { null },
                        storeAddress = storeAddress.ifBlank { null }
                    )
                    itemToUpdatePrice = null
                }
            )
        }

        // Add Store Dialog
        if (showAddStoreDialog) {
            AddStoreDialog(
                currentLocation = currentLocation,
                isLocating = isLocating,
                onDetectLocation = { viewModel.detectCurrentLocation() },
                onDismiss = { showAddStoreDialog = false },
                onConfirm = { name, address, lat, lng, emoji ->
                    viewModel.addOrUpdateStore(name, address, lat, lng, emoji)
                    showAddStoreDialog = false
                }
            )
        }

        // Budget Limiter Dialog
        if (showBudgetDialog) {
            BudgetDialog(
                currentBudget = budgetLimit,
                currency = currency,
                onDismiss = { showBudgetDialog = false },
                onSetBudget = { newBudget ->
                    viewModel.setBudgetLimit(newBudget)
                    showBudgetDialog = false
                }
            )
        }

        // Currency & Location Pricing Dialog
        if (showCurrencyDialog) {
            CurrencyDialog(
                currentCurrency = currency,
                onDetectFromGps = { viewModel.detectCurrentLocation() },
                onSelectCurrency = { newCurr ->
                    viewModel.setCurrency(newCurr)
                },
                onDismiss = { showCurrencyDialog = false }
            )
        }

        // Family Sync & Cross-User Dialog
        if (showFamilySyncDialog) {
            FamilySyncDialog(
                itemCount = totals.totalItemCount,
                onExportText = { viewModel.exportCartAsShareableText() },
                onExportJson = { viewModel.exportFamilySyncPayload() },
                onImportJson = { jsonStr, merge ->
                    viewModel.importFamilySyncPayload(jsonStr, merge)
                },
                onDismiss = { showFamilySyncDialog = false }
            )
        }
    }
}
