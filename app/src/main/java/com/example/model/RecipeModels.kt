package com.example.model

data class RecipeIngredient(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val estimatedPrice: Double = 0.0,
    val category: String = "Pantry",
    val isSelected: Boolean = true
)

data class RecipeResult(
    val title: String,
    val description: String,
    val servings: String = "4 servings",
    val prepTime: String = "30 mins",
    val ingredients: List<RecipeIngredient>,
    val instructions: List<String> = emptyList(),
    val source: String = "AI Generated"
)

data class SuggestedRecipe(
    val title: String,
    val description: String,
    val prepTime: String,
    val servings: String,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>,
    val matchPercentage: Int,
    val instructions: List<String>
)
