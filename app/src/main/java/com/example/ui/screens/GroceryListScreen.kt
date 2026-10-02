package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.GroceryItem
import com.example.data.entity.Store
import com.example.ui.GroceryTotals
import com.example.ui.GroceryViewModel
import com.example.ui.components.CATEGORIES
import com.example.ui.components.GroceryItemCard
import com.example.ui.components.TotalsBanner

@Composable
fun GroceryListScreen(
    viewModel: GroceryViewModel,
    onAddItemClick: () -> Unit,
    onEditItemClick: (GroceryItem) -> Unit,
    onUpdatePriceClick: (GroceryItem) -> Unit,
    onOpenBudgetDialog: () -> Unit,
    onOpenCurrencyDialog: () -> Unit,
    onOpenFamilySyncDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val stores by viewModel.allStores.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStoreFilter by viewModel.selectedStoreFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val locationMessage by viewModel.locationMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(locationMessage) {
        locationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLocationMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItemClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Item", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_item")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Totals Banner
            item(key = "totals_banner") {
                TotalsBanner(
                    totals = totals,
                    currentLocation = currentLocation,
                    onOpenBudgetDialog = onOpenBudgetDialog,
                    onOpenCurrencyDialog = onOpenCurrencyDialog,
                    onOpenFamilySyncDialog = onOpenFamilySyncDialog,
                    onClearChecked = { viewModel.clearCheckedItems() },
                    onUncheckAll = { viewModel.uncheckAllItems() },
                    onClearAll = { viewModel.clearAllItems() }
                )
            }

            // Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search items, stores, or categories...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grocery_search_input")
                )
            }

            // Store Filter Row
            item(key = "store_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedStoreFilter == null,
                            onClick = { viewModel.setStoreFilter(null) },
                            label = { Text("All Stores (${allItems.size})") },
                            modifier = Modifier.testTag("filter_all_stores")
                        )

                        stores.forEach { store ->
                            val count = allItems.count { it.storeName.equals(store.name, ignoreCase = true) }
                            FilterChip(
                                selected = selectedStoreFilter.equals(store.name, ignoreCase = true),
                                onClick = {
                                    if (selectedStoreFilter.equals(store.name, ignoreCase = true)) {
                                        viewModel.setStoreFilter(null)
                                    } else {
                                        viewModel.setStoreFilter(store.name)
                                    }
                                },
                                leadingIcon = { Text(store.iconEmoji) },
                                label = { Text("${store.name} ($count)") },
                                modifier = Modifier.testTag("filter_store_${store.id}")
                            )
                        }
                    }

                    // Category Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All Categories") }
                        )

                        CATEGORIES.forEach { (catName, emoji) ->
                            val count = allItems.count { it.category.equals(catName, ignoreCase = true) }
                            if (count > 0 || selectedCategoryFilter.equals(catName, ignoreCase = true)) {
                                FilterChip(
                                    selected = selectedCategoryFilter.equals(catName, ignoreCase = true),
                                    onClick = {
                                        if (selectedCategoryFilter.equals(catName, ignoreCase = true)) {
                                            viewModel.setCategoryFilter(null)
                                        } else {
                                            viewModel.setCategoryFilter(catName)
                                        }
                                    },
                                    leadingIcon = { Text(emoji) },
                                    label = { Text("$catName ($count)") }
                                )
                            }
                        }
                    }
                }
            }

            // Items List or Empty State
            if (items.isEmpty()) {
                item(key = "empty_state") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("empty_cart_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedStoreFilter != null || selectedCategoryFilter != null) {
                                    "No matching items found"
                                } else {
                                    "Your grocery list is empty"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedStoreFilter != null) {
                                    "Try clearing your search or filters to see all items."
                                } else {
                                    "Tap '+ Add Item' to start adding products and tracking prices with store location tags."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (searchQuery.isNotBlank() || selectedStoreFilter != null || selectedCategoryFilter != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.setSearchQuery("")
                                        viewModel.setStoreFilter(null)
                                        viewModel.setCategoryFilter(null)
                                    }
                                ) {
                                    Text("Reset all filters")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    GroceryItemCard(
                        item = item,
                        currencySymbol = totals.currency.symbol,
                        onToggleCheck = { viewModel.toggleItemChecked(item) },
                        onIncrementQty = { viewModel.updateQuantity(item.id, item.quantity + 1.0) },
                        onDecrementQty = { viewModel.updateQuantity(item.id, (item.quantity - 1.0).coerceAtLeast(0.0)) },
                        onUpdatePrice = { onUpdatePriceClick(item) },
                        onEditItem = { onEditItemClick(item) },
                        onDeleteItem = { viewModel.deleteItem(item) },
                        onStoreClick = { store ->
                            viewModel.setStoreFilter(store)
                        }
                    )
                }
            }
        }
    }
}
