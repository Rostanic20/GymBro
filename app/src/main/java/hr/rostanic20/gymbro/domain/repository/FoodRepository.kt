package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FoodRepository {
    fun foods(): Flow<List<Food>>
    fun recipes(): Flow<List<Recipe>>
    fun log(date: LocalDate): Flow<List<FoodLogEntry>>
    suspend fun food(id: Long): Food?
    suspend fun saveFood(draft: FoodDraft): Long
    suspend fun setFavourite(foodId: Long, favourite: Boolean)
    suspend fun logFood(date: LocalDate, meal: Meal, food: Food, grams: Double, nowMillis: Long)
    suspend fun logRecipe(date: LocalDate, meal: Meal, recipe: Recipe, nowMillis: Long)
    suspend fun logEstimate(date: LocalDate, meal: Meal, name: String, nutrition: Nutrition, nowMillis: Long)
    suspend fun repeatMeal(from: LocalDate, to: LocalDate, meal: Meal, nowMillis: Long): Int
    suspend fun deleteEntry(entryId: Long)
}
