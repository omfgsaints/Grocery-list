package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.GroceryItem
import com.example.data.entity.PriceRecord
import com.example.data.entity.Store
import com.example.location.LocationResult
import kotlinx.coroutines.launch
import java.util.Locale

val CATEGORIES = listOf(
    "Produce" to "🥬",
    "Dairy" to "🧀",
    "Bakery" to "🍞",
    "Meat" to "🥩",
    "Pantry" to "🥫",
    "Frozen" to "🍦",
    "Beverages" to "🧃",
    "Snacks" to "🥨",
    "Household" to "🧼"
)

val COMMON_UNITS = listOf("pcs", "lbs", "kg", "gal", "oz", "dozen", "pack", "bag", "can", "box")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditItemDialog(
    itemToEdit: GroceryItem? = null,
    stores: List<Store>,
    currentLocation: LocationResult?,
    currencySymbol: String = "₱",
    isLocating: Boolean,
    onDetectLocation: () -> Unit,
    onPriceLookup: suspend (String, String?) -> PriceRecord?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        price: Double,
        storeName: String,
        storeAddress: String,
        lat: Double?,
        lng: Double?,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Produce") }
    var quantityText by remember { mutableStateOf(if (itemToEdit != null) formatQty(itemToEdit.quantity) else "1") }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "pcs") }
    var priceText by remember { mutableStateOf(if (itemToEdit != null && itemToEdit.price > 0) String.format(Locale.US, "%.2f", itemToEdit.price) else "") }
    var storeName by remember { mutableStateOf(itemToEdit?.storeName ?: "") }
    var storeAddress by remember { mutableStateOf(itemToEdit?.storeAddress ?: "") }
    var latitude by remember { mutableStateOf(itemToEdit?.latitude) }
    var longitude by remember { mutableStateOf(itemToEdit?.longitude) }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    var suggestedPriceRecord by remember { mutableStateOf<PriceRecord?>(null) }
    val scope = rememberCoroutineScope()

    // Trigger price suggestion lookup whenever product name or store changes
    LaunchedEffect(name, storeName) {
        if (name.trim().length >= 2) {
            suggestedPriceRecord = onPriceLookup(name.trim(), storeName.ifBlank { null })
        } else {
            suggestedPriceRecord = null
        }
    }

    // If current location changes and user hasn't set a store, propose it
    LaunchedEffect(currentLocation) {
        if (currentLocation != null && storeName.isBlank() && itemToEdit == null) {
            storeName = currentLocation.suggestedStoreName
            storeAddress = currentLocation.address
            latitude = currentLocation.latitude
            longitude = currentLocation.longitude
        }
    }

    val quantity = quantityText.toDoubleOrNull() ?: 1.0
    val unitPrice = priceText.toDoubleOrNull() ?: 0.0
    val totalCost = quantity * unitPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (itemToEdit == null) "Add Grocery Item" else "Edit Item & Price",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g., Organic Whole Milk") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear name")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )

                // Price suggestion banner if available
                AnimatedVisibility(visible = suggestedPriceRecord != null && unitPrice == 0.0) {
                    suggestedPriceRecord?.let { record ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = "Suggestion",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Saved price at ${record.storeName}: $currencySymbol${String.format(Locale.US, "%.2f", record.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        priceText = String.format(Locale.US, "%.2f", record.price)
                                        unit = record.unit
                                        if (storeName.isBlank()) {
                                            storeName = record.storeName
                                            storeAddress = record.storeAddress
                                            latitude = record.latitude
                                            longitude = record.longitude
                                        }
                                    },
                                    modifier = Modifier.testTag("apply_suggestion_button")
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Quantity & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stepper row
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quantity_input"),
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    val current = quantityText.toDoubleOrNull() ?: 1.0
                                    val newQ = (current - 1.0).coerceAtLeast(1.0)
                                    quantityText = formatQty(newQ)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val current = quantityText.toDoubleOrNull() ?: 1.0
                                    quantityText = formatQty(current + 1.0)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    )

                    // Unit picker
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier
                            .width(100.dp)
                            .testTag("unit_input")
                    )
                }

                // Common Unit quick chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    COMMON_UNITS.forEach { u ->
                        FilterChip(
                            selected = unit.equals(u, ignoreCase = true),
                            onClick = { unit = u },
                            label = { Text(u, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Unit Price & Total Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price per $unit") },
                        placeholder = { Text("0.00") },
                        prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("price_input")
                    )

                    // Live Total Calculation Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Item Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%.2f", totalCost)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Store & Location Section
                Text(
                    text = "Store & Location Tag",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name") },
                    placeholder = { Text("e.g., Trader Joe's, Costco") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_name_input")
                )

                // Current GPS Location tagging button
                OutlinedButton(
                    onClick = onDetectLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detect_location_button")
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detecting GPS Location...")
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLocation != null) "📍 Use Current Location (${currentLocation.suggestedStoreName})" else "📍 Tag My Current Location"
                        )
                    }
                }

                // If store address exists
                if (storeAddress.isNotBlank()) {
                    Text(
                        text = "Address: $storeAddress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Existing stores quick chips
                if (stores.isNotEmpty()) {
                    Text(
                        text = "Select from saved stores:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stores.forEach { store ->
                            FilterChip(
                                selected = storeName.equals(store.name, ignoreCase = true),
                                onClick = {
                                    storeName = store.name
                                    storeAddress = store.address
                                    latitude = store.latitude
                                    longitude = store.longitude
                                },
                                leadingIcon = {
                                    Text(store.iconEmoji)
                                },
                                label = { Text(store.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Category Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIES.forEach { (catName, emoji) ->
                        FilterChip(
                            selected = category.equals(catName, ignoreCase = true),
                            onClick = { category = catName },
                            leadingIcon = { Text(emoji) },
                            label = { Text(catName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Brand, aisle, size details...") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            category,
                            quantity,
                            unit.trim().ifBlank { "pcs" },
                            unitPrice,
                            storeName.trim(),
                            storeAddress.trim(),
                            latitude,
                            longitude,
                            notes.trim()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_item_button")
            ) {
                Text(if (itemToEdit == null) "Add to List" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_item_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickPriceUpdateDialog(
    item: GroceryItem,
    currentLocation: LocationResult?,
    currencySymbol: String = "₱",
    onDismiss: () -> Unit,
    onConfirm: (newPrice: Double, storeName: String, storeAddress: String) -> Unit
) {
    var newPriceText by remember {
        mutableStateOf(if (item.price > 0) String.format(Locale.US, "%.2f", item.price) else "")
    }
    var storeName by remember {
        mutableStateOf(item.storeName.ifBlank { currentLocation?.suggestedStoreName ?: "" })
    }
    var storeAddress by remember {
        mutableStateOf(item.storeAddress.ifBlank { currentLocation?.address ?: "" })
    }

    val currentPrice = item.price
    val newPrice = newPriceText.toDoubleOrNull() ?: 0.0
    val diff = newPrice - currentPrice
    val pct = if (currentPrice > 0) (diff / currentPrice) * 100 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Price for ${item.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Previous price: $currencySymbol${String.format(Locale.US, "%.2f", currentPrice)} / ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = newPriceText,
                    onValueChange = { newPriceText = it },
                    label = { Text("New Price per ${item.unit} *") },
                    prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_price_input")
                )

                // Price Difference indicator
                if (newPrice > 0 && currentPrice > 0 && diff != 0.0) {
                    val isCheaper = diff < 0
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isCheaper) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCheaper) "📉 Price Decreased!" else "📈 Price Increased",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCheaper) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${if (diff > 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%.2f", diff)} (${String.format(Locale.US, "%.1f", pct)}%)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCheaper) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Store tag
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store / Location Tag") },
                    placeholder = { Text("e.g., Trader Joe's") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentLocation != null && !storeName.equals(currentLocation.suggestedStoreName, ignoreCase = true)) {
                    AssistChip(
                        onClick = {
                            storeName = currentLocation.suggestedStoreName
                            storeAddress = currentLocation.address
                        },
                        label = { Text("Tag current: ${currentLocation.suggestedStoreName}") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Text(
                    text = "Saving this will update the item on your list and record the new price in your price history catalog for future reference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPrice >= 0) {
                        onConfirm(newPrice, storeName.trim(), storeAddress.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_price_update_button")
            ) {
                Text("Save to Price History")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddStoreDialog(
    currentLocation: LocationResult?,
    isLocating: Boolean,
    onDetectLocation: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String, lat: Double, lng: Double, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf(currentLocation?.suggestedStoreName ?: "") }
    var address by remember { mutableStateOf(currentLocation?.address ?: "") }
    var latitude by remember { mutableDoubleStateOf(currentLocation?.latitude ?: 0.0) }
    var longitude by remember { mutableDoubleStateOf(currentLocation?.longitude ?: 0.0) }
    var emoji by remember { mutableStateOf("🛒") }

    val emojis = listOf("🛒", "🌿", "🍎", "📦", "🏪", "🥩", "🍞", "☕", "💊")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add / Tag Store Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Store Name *") },
                    placeholder = { Text("e.g. Trader Joe's Downtown") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_store_name_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Store Address / Area") },
                    placeholder = { Text("123 Market St, City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_store_address_input")
                )

                OutlinedButton(
                    onClick = onDetectLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detecting...")
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fill with Current GPS Location")
                    }
                }

                if (latitude != 0.0) {
                    Text(
                        text = "GPS: ${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Store Icon:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojis.forEach { e ->
                        FilterChip(
                            selected = emoji == e,
                            onClick = { emoji = e },
                            label = { Text(e, style = MaterialTheme.typography.titleSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), address.trim(), latitude, longitude, emoji)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_store_button")
            ) {
                Text("Save Store")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        qty.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", qty)
    }
}
