package com.example.ai

import com.example.BuildConfig
import com.example.model.RecipeIngredient
import com.example.model.RecipeResult
import com.example.model.SuggestedRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiRecipeService {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun searchRecipeIngredients(query: String, currencySymbol: String): RecipeResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (isApiKeyConfigured()) {
            try {
                val prompt = """
                    Provide the recipe ingredients and basic details for '$trimmed'.
                    Use currency symbol '$currencySymbol' for estimated unit prices.
                    Return ONLY a JSON object with:
                    {
                      "title": "Dish name",
                      "description": "Short appetizing description",
                      "servings": "4 servings",
                      "prepTime": "30 mins",
                      "ingredients": [
                        {
                          "name": "Ingredient name",
                          "quantity": 1.0,
                          "unit": "lbs/kg/pcs/tbsp/cup/can",
                          "estimatedPrice": 2.50,
                          "category": "Produce/Meat/Dairy/Pantry/Frozen"
                        }
                      ],
                      "instructions": [
                        "Step 1 description...",
                        "Step 2 description..."
                      ]
                    }
                """.trimIndent()

                val responseJson = callGeminiApi(prompt)
                val parsed = parseRecipeResult(responseJson, trimmed, currencySymbol)
                if (parsed != null && parsed.ingredients.isNotEmpty()) {
                    return@withContext parsed
                }
            } catch (e: Exception) {
                // Fallback to offline knowledge base
            }
        }
        return@withContext getOfflineRecipe(trimmed, currencySymbol)
    }

    suspend fun suggestRecipesFromIngredients(
        boughtItems: List<String>,
        currencySymbol: String
    ): List<SuggestedRecipe> = withContext(Dispatchers.IO) {
        if (boughtItems.isEmpty()) {
            return@withContext getPopularFilipinoAndGlobalSuggestions(emptyList())
        }

        if (isApiKeyConfigured()) {
            try {
                val prompt = """
                    I have bought or currently have these grocery items: ${boughtItems.joinToString(", ")}.
                    Suggest 3 delicious, practical recipes I can cook using these bought items.
                    Return ONLY a valid JSON array of objects with schema:
                    [
                      {
                        "title": "Recipe Title",
                        "description": "Short description of the dish",
                        "prepTime": "25 mins",
                        "servings": "3-4 servings",
                        "matchedIngredients": ["Item from bought list used", ...],
                        "missingIngredients": ["1 or 2 small pantry staples needed", ...],
                        "matchPercentage": 85,
                        "instructions": ["Step 1", "Step 2", "Step 3"]
                      }
                    ]
                """.trimIndent()

                val responseJson = callGeminiApi(prompt)
                val list = parseSuggestedRecipes(responseJson)
                if (list.isNotEmpty()) {
                    return@withContext list
                }
            } catch (e: Exception) {
                // Fallback to offline matcher
            }
        }

        return@withContext getPopularFilipinoAndGlobalSuggestions(boughtItems)
    }

    private fun callGeminiApi(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // generationConfig
            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            }
            put("generationConfig", genConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val root = JSONObject(body)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }

    private fun parseRecipeResult(rawJson: String, fallbackQuery: String, currencySymbol: String): RecipeResult? {
        return try {
            val root = JSONObject(rawJson)
            val title = root.optString("title", fallbackQuery)
            val description = root.optString("description", "Delicious homecooked dish")
            val servings = root.optString("servings", "4 servings")
            val prepTime = root.optString("prepTime", "35 mins")

            val ingredientsList = mutableListOf<RecipeIngredient>()
            val ingArray = root.optJSONArray("ingredients")
            if (ingArray != null) {
                for (i in 0 until ingArray.length()) {
                    val ingObj = ingArray.getJSONObject(i)
                    ingredientsList.add(
                        RecipeIngredient(
                            name = ingObj.optString("name", "Ingredient"),
                            quantity = ingObj.optDouble("quantity", 1.0),
                            unit = ingObj.optString("unit", "pcs"),
                            estimatedPrice = ingObj.optDouble("estimatedPrice", 50.0),
                            category = ingObj.optString("category", "Pantry"),
                            isSelected = true
                        )
                    )
                }
            }

            val instructionsList = mutableListOf<String>()
            val instArray = root.optJSONArray("instructions")
            if (instArray != null) {
                for (i in 0 until instArray.length()) {
                    instructionsList.add(instArray.getString(i))
                }
            }

            RecipeResult(
                title = title,
                description = description,
                servings = servings,
                prepTime = prepTime,
                ingredients = ingredientsList,
                instructions = instructionsList,
                source = "Gemini 3.5 AI"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSuggestedRecipes(rawJson: String): List<SuggestedRecipe> {
        return try {
            val array = if (rawJson.trim().startsWith("[")) {
                JSONArray(rawJson)
            } else {
                val obj = JSONObject(rawJson)
                obj.optJSONArray("recipes") ?: JSONArray()
            }

            val list = mutableListOf<SuggestedRecipe>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.optString("title", "Delicious Meal")
                val desc = obj.optString("description", "")
                val prepTime = obj.optString("prepTime", "30 mins")
                val servings = obj.optString("servings", "4 servings")
                val match = obj.optInt("matchPercentage", 80)

                val matched = mutableListOf<String>()
                val matchedArr = obj.optJSONArray("matchedIngredients")
                if (matchedArr != null) {
                    for (j in 0 until matchedArr.length()) matched.add(matchedArr.getString(j))
                }

                val missing = mutableListOf<String>()
                val missingArr = obj.optJSONArray("missingIngredients")
                if (missingArr != null) {
                    for (j in 0 until missingArr.length()) missing.add(missingArr.getString(j))
                }

                val instructions = mutableListOf<String>()
                val instArr = obj.optJSONArray("instructions")
                if (instArr != null) {
                    for (j in 0 until instArr.length()) instructions.add(instArr.getString(j))
                }

                list.add(
                    SuggestedRecipe(
                        title = title,
                        description = desc,
                        prepTime = prepTime,
                        servings = servings,
                        matchedIngredients = matched,
                        missingIngredients = missing,
                        matchPercentage = match,
                        instructions = instructions
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Curated rich recipe database (includes iconic Philippine recipes and global favorites)
    private fun getOfflineRecipe(query: String, currencySymbol: String): RecipeResult {
        val q = query.lowercase()
        val isPhp = currencySymbol == "₱"
        val rate = if (isPhp) 55.0 else 1.0

        if (q.contains("adobo")) {
            return RecipeResult(
                title = "Classic Filipino Chicken & Pork Adobo",
                description = "Iconic Philippine dish braised in savory soy sauce, vinegar, crushed garlic, and bay leaves.",
                servings = "4 - 5 servings",
                prepTime = "40 mins",
                ingredients = listOf(
                    RecipeIngredient("Chicken thighs or pork belly", 1.0, if (isPhp) "kg" else "lbs", 240.0 / (if (isPhp) 1.0 else rate), "Meat"),
                    RecipeIngredient("Soy Sauce", 1.0, "bottle", 35.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Cane Vinegar", 1.0, "bottle", 30.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Garlic head", 2.0, "pcs", 20.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Whole Black Peppercorns", 1.0, "pack", 15.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Dried Bay Leaves (Laurel)", 1.0, "pack", 15.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Cooking Oil", 1.0, "bottle", 50.0 / (if (isPhp) 1.0 else rate), "Pantry")
                ),
                instructions = listOf(
                    "Marinate meat in soy sauce, vinegar, crushed garlic, and peppercorns for at least 20 minutes.",
                    "Heat oil in a pan and sear the meat until lightly browned on all sides.",
                    "Pour in the marinade and bay leaves. Bring to a boil without stirring for 3 minutes.",
                    "Lower heat, cover, and simmer for 25-30 minutes until meat is tender and sauce reduces to a rich glaze.",
                    "Serve hot with steamed white rice!"
                ),
                source = "Classic Philippine Cuisine"
            )
        } else if (q.contains("sinigang")) {
            return RecipeResult(
                title = "Pork Sinigang (Sour Tamarind Soup)",
                description = "Comforting Philippine sour soup bursting with savory pork, crisp kangkong, and tangy tamarind broth.",
                servings = "5 servings",
                prepTime = "45 mins",
                ingredients = listOf(
                    RecipeIngredient("Pork Ribs / Pork Belly", 1.0, if (isPhp) "kg" else "lbs", 320.0 / (if (isPhp) 1.0 else rate), "Meat"),
                    RecipeIngredient("Tamarind Soup Base (Sinigang mix)", 2.0, "packs", 40.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Kangkong (Water Spinach)", 1.0, "bunch", 25.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Tomatoes", 4.0, "pcs", 30.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Onion", 2.0, "pcs", 25.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Radish (Labanos)", 1.0, "pc", 25.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Green Chili (Siling Pangsigang)", 3.0, "pcs", 15.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Fish Sauce (Patis)", 1.0, "bottle", 35.0 / (if (isPhp) 1.0 else rate), "Pantry")
                ),
                instructions = listOf(
                    "In a large pot, bring water to boil with sliced onions and tomatoes.",
                    "Add pork ribs and simmer on medium-low for 35 minutes until tender.",
                    "Add radish and green finger chilies; cook for 5 minutes.",
                    "Stir in the tamarind sinigang mix and season with patis to taste.",
                    "Turn off heat, add fresh kangkong leaves, cover pot, and serve piping hot!"
                ),
                source = "Authentic Filipino Recipes"
            )
        } else if (q.contains("pancit") || q.contains("bihon") || q.contains("canton")) {
            return RecipeResult(
                title = "Pancit Bihon Guisado",
                description = "Filipino celebration stir-fried noodles with chicken, pork, and colorful fresh vegetables.",
                servings = "6 servings",
                prepTime = "30 mins",
                ingredients = listOf(
                    RecipeIngredient("Bihon Rice Noodles", 1.0, "pack (500g)", 55.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Chicken breast", 0.5, if (isPhp) "kg" else "lbs", 140.0 / (if (isPhp) 1.0 else rate), "Meat"),
                    RecipeIngredient("Carrots (julienned)", 2.0, "pcs", 30.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Cabbage (shredded)", 1.0, "head", 45.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Snow peas (chicharo)", 1.0, "pack", 35.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Calamansi / Lemon", 6.0, "pcs", 20.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Oyster Sauce & Soy Sauce", 1.0, "bottle", 45.0 / (if (isPhp) 1.0 else rate), "Pantry")
                ),
                instructions = listOf(
                    "Soak bihon noodles in water for 5 minutes and drain.",
                    "Sauté garlic and onions, add chicken strips, and cook until browned.",
                    "Toss in carrots, snow peas, and cabbage with soy sauce and oyster sauce. Remove vegetables once tender-crisp.",
                    "Add broth to the pan, bring to a boil, then mix in noodles until broth is absorbed.",
                    "Toss cooked vegetables back in and serve with fresh calamansi halves."
                ),
                source = "Filipino Celebrations"
            )
        } else {
            // Generic intelligent fallback
            return RecipeResult(
                title = query.replaceFirstChar { it.uppercase() },
                description = "Delicious, home-style recipe prepared with fresh market ingredients.",
                servings = "3 - 4 servings",
                prepTime = "30 mins",
                ingredients = listOf(
                    RecipeIngredient("Main protein or vegetable for $query", 1.0, if (isPhp) "kg" else "lbs", 180.0 / (if (isPhp) 1.0 else rate), "Meat"),
                    RecipeIngredient("Garlic & Onions", 1.0, "pack", 30.0 / (if (isPhp) 1.0 else rate), "Produce"),
                    RecipeIngredient("Cooking Oil & Seasonings", 1.0, "pack", 45.0 / (if (isPhp) 1.0 else rate), "Pantry"),
                    RecipeIngredient("Fresh herbs or garnishes", 1.0, "bunch", 25.0 / (if (isPhp) 1.0 else rate), "Produce")
                ),
                instructions = listOf(
                    "Prepare and wash all fresh ingredients thoroughly.",
                    "Sauté aromatic base (garlic, onion, herbs) in a pan with cooking oil.",
                    "Add main protein or vegetables and cook over medium heat until golden and tender.",
                    "Season to taste and serve with rice or fresh bread."
                ),
                source = "Smart Recipe Suggestion"
            )
        }
    }

    private fun getPopularFilipinoAndGlobalSuggestions(boughtItems: List<String>): List<SuggestedRecipe> {
        val lowerItems = boughtItems.map { it.lowercase() }

        fun matchesAny(vararg keywords: String): List<String> {
            val matched = mutableListOf<String>()
            for (kw in keywords) {
                val found = boughtItems.firstOrNull { it.lowercase().contains(kw) }
                if (found != null) matched.add(found)
            }
            return matched.distinct()
        }

        val adoboMatched = matchesAny("chicken", "pork", "meat", "garlic", "soy", "vinegar")
        val sinigangMatched = matchesAny("pork", "shrimp", "fish", "tomato", "onion", "kangkong", "radish")
        val breakfastMatched = matchesAny("egg", "eggs", "milk", "bread", "banana", "butter")

        return listOf(
            SuggestedRecipe(
                title = "Savory Chicken / Pork Adobo",
                description = "The ultimate Filipino home comfort meal with garlic, vinegar, and soy glaze.",
                prepTime = "35 mins",
                servings = "4 servings",
                matchedIngredients = if (adoboMatched.isNotEmpty()) adoboMatched else listOf("Chicken / Pork"),
                missingIngredients = listOf("Garlic", "Soy Sauce", "Vinegar", "Bay Leaves") - adoboMatched.toSet(),
                matchPercentage = if (adoboMatched.size >= 2) 85 else 60,
                instructions = listOf(
                    "Brown meat in a hot skillet with crushed garlic.",
                    "Add soy sauce, cane vinegar, bay leaves, and cracked black pepper.",
                    "Simmer gently for 25 minutes until tender and caramelized."
                )
            ),
            SuggestedRecipe(
                title = "Tangy Sinigang Stew",
                description = "A refreshing, comforting sour soup packed with tender meat and leafy greens.",
                prepTime = "40 mins",
                servings = "4-5 servings",
                matchedIngredients = if (sinigangMatched.isNotEmpty()) sinigangMatched else listOf("Pork / Protein"),
                missingIngredients = listOf("Sinigang mix", "Tomatoes", "Kangkong") - sinigangMatched.toSet(),
                matchPercentage = if (sinigangMatched.size >= 2) 80 else 55,
                instructions = listOf(
                    "Boil meat in a pot with chopped ripe tomatoes and onions.",
                    "Add sinigang tamarind soup mix and vegetables.",
                    "Simmer until vegetables are bright and tender. Serve warm with rice."
                )
            ),
            SuggestedRecipe(
                title = "Hearty Market Omelette & Toast",
                description = "Quick and satisfying protein breakfast using farm fresh eggs and fresh ingredients.",
                prepTime = "15 mins",
                servings = "2 servings",
                matchedIngredients = if (breakfastMatched.isNotEmpty()) breakfastMatched else listOf("Eggs", "Bread"),
                missingIngredients = listOf("Salt", "Pepper", "Cooking Butter") - breakfastMatched.toSet(),
                matchPercentage = if (breakfastMatched.isNotEmpty()) 90 else 70,
                instructions = listOf(
                    "Whisk eggs with a splash of milk and pinch of salt.",
                    "Pour into buttered skillet and fold gently.",
                    "Serve with toasted bread and fresh fruit."
                )
            )
        )
    }
}
