package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RecipeIngredient
import com.example.model.RecipeResult
import com.example.model.SuggestedRecipe
import com.example.ui.GroceryViewModel
import com.example.ui.RecipeSearchUiState
import com.example.ui.RecipeSuggestUiState
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun RecipeAiScreen(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var recipeQuery by remember { mutableStateOf("") }
    val searchState by viewModel.recipeSearchState.collectAsStateWithLifecycle()
    val suggestState by viewModel.recipeSuggestState.collectAsStateWithLifecycle()
    val currency by viewModel.currentCurrency.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val quickRecipeIdeas = listOf(
        "Chicken Adobo 🇵🇭",
        "Pork Sinigang 🍲",
        "Pancit Bihon 🍜",
        "Kare-Kare 🥜",
        "Carbonara Pasta 🍝",
        "Beef Steak (Bistek) 🥩",
        "Mango Float 🍨"
    )

    LaunchedEffect(Unit) {
        if (suggestState is RecipeSuggestUiState.Idle) {
            viewModel.loadRecipeSuggestionsForBoughtItems()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                    text = { Text("Search Recipe Ingredients") },
                    modifier = Modifier.testTag("tab_recipe_ingredients")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.loadRecipeSuggestionsForBoughtItems()
                    },
                    icon = { Icon(Icons.Default.Kitchen, contentDescription = null) },
                    text = { Text("Cook What You Bought") },
                    modifier = Modifier.testTag("tab_cook_what_bought")
                )
            }

            if (selectedTab == 0) {
                // Tab 1: Recipe Ingredient Search
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "recipe_search_header") {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Recipe Ingredient Finder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Enter any dish you crave. AI generates the ingredients, and you can add what you need to your grocery list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    item(key = "recipe_search_input") {
                        OutlinedTextField(
                            value = recipeQuery,
                            onValueChange = { recipeQuery = it },
                            placeholder = { Text("e.g. Chicken Adobo, Sinigang, Lasagna...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            },
                            trailingIcon = {
                                if (recipeQuery.isNotEmpty()) {
                                    IconButton(onClick = { recipeQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recipe_query_input")
                        )
                    }

                    item(key = "quick_recipe_chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickRecipeIdeas.forEach { idea ->
                                val cleanName = idea.replace(Regex("[^a-zA-Z0-9 -]"), "").trim()
                                FilterChip(
                                    selected = recipeQuery.equals(cleanName, ignoreCase = true),
                                    onClick = {
                                        recipeQuery = cleanName
                                        viewModel.searchRecipeIngredients(cleanName)
                                    },
                                    label = { Text(idea, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    item(key = "recipe_search_button") {
                        Button(
                            onClick = {
                                if (recipeQuery.isNotBlank()) {
                                    viewModel.searchRecipeIngredients(recipeQuery.trim())
                                }
                            },
                            enabled = recipeQuery.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_recipe_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Ingredients with AI")
                        }
                    }

                    // Results
                    when (val state = searchState) {
                        is RecipeSearchUiState.Loading -> {
                            item(key = "recipe_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Consulting AI culinary chef...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        is RecipeSearchUiState.Success -> {
                            item(key = "recipe_result_card") {
                                RecipeResultCard(
                                    recipe = state.recipe,
                                    currencySymbol = currency.symbol,
                                    onAddSelectedToList = { selectedIngredients ->
                                        viewModel.addRecipeIngredientsToList(selectedIngredients)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added ${selectedIngredients.size} items to your Grocery List!")
                                        }
                                    }
                                )
                            }
                        }
                        is RecipeSearchUiState.Error -> {
                            item(key = "recipe_error") {
                                Text(
                                    text = "Error: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        is RecipeSearchUiState.Idle -> {}
                    }
                }
            } else {
                // Tab 2: Cook What You Bought
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "suggest_header") {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cook With What You Bought",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val boughtCount = allItems.count { it.isChecked }
                            Text(
                                text = "Based on the $boughtCount checked item${if (boughtCount != 1) "s" else ""} in your cart (or items on your list).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadRecipeSuggestionsForBoughtItems() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("refresh_suggestions_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Fresh Meal Ideas")
                            }
                        }
                    }

                    when (val state = suggestState) {
                        is RecipeSuggestUiState.Loading -> {
                            item(key = "suggest_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Matching meals to your cart...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        is RecipeSuggestUiState.Success -> {
                            items(
                                items = state.recipes,
                                key = { it.title }
                            ) { recipe ->
                                SuggestedRecipeCard(
                                    recipe = recipe,
                                    onAddMissingToList = { missingList ->
                                        val ingredients = missingList.map { name ->
                                            RecipeIngredient(
                                                name = name,
                                                quantity = 1.0,
                                                unit = "pcs",
                                                estimatedPrice = 50.0,
                                                category = "Pantry",
                                                isSelected = true
                                            )
                                        }
                                        viewModel.addRecipeIngredientsToList(ingredients)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added missing ingredients to your cart!")
                                        }
                                    }
                                )
                            }
                        }
                        is RecipeSuggestUiState.Error -> {
                            item(key = "suggest_error") {
                                Text(
                                    text = "Error: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        is RecipeSuggestUiState.Idle -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeResultCard(
    recipe: RecipeResult,
    currencySymbol: String,
    onAddSelectedToList: (List<RecipeIngredient>) -> Unit,
    modifier: Modifier = Modifier
) {
    val ingredients = remember(recipe) {
        mutableStateListOf<RecipeIngredient>().apply {
            addAll(recipe.ingredients)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recipe_result_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⏱ ${recipe.prepTime}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "🍽 ${recipe.servings}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ingredients (Select what you need to buy):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            ingredients.forEachIndexed { index, ing ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            ingredients[index] = ing.copy(isSelected = !ing.isSelected)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = ing.isSelected,
                        onCheckedChange = { checked ->
                            ingredients[index] = ing.copy(isSelected = checked)
                        },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ing.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${ing.quantity} ${ing.unit} • ${ing.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (ing.estimatedPrice > 0) {
                        Text(
                            text = "~$currencySymbol${String.format(Locale.US, "%.2f", ing.estimatedPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Cooking Steps
            if (recipe.instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preparation Instructions:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                recipe.instructions.forEachIndexed { idx, step ->
                    Text(
                        text = "${idx + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val selectedCount = ingredients.count { it.isSelected }
            Button(
                onClick = { onAddSelectedToList(ingredients.filter { it.isSelected }) },
                enabled = selectedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_ingredients_to_cart_btn")
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add $selectedCount Ingredients to Grocery List")
            }
        }
    }
}

@Composable
fun SuggestedRecipeCard(
    recipe: SuggestedRecipe,
    onAddMissingToList: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("suggested_recipe_${recipe.title.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⏱ ${recipe.prepTime} • ${recipe.servings}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${recipe.matchPercentage}% Match",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Matched from your cart
            Text(
                text = "✅ In Your Cart: ${recipe.matchedIngredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            // Missing
            if (recipe.missingIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🛒 Missing: ${recipe.missingIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide Recipe Steps" else "View Recipe Steps")
                }

                if (recipe.missingIngredients.isNotEmpty()) {
                    Button(
                        onClick = { onAddMissingToList(recipe.missingIngredients) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add Missing (${recipe.missingIngredients.size})")
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    recipe.instructions.forEachIndexed { i, step ->
                        Text(
                            text = "${i + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
