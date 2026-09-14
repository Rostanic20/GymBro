package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.FoodLocalDataSource
import hr.rostanic20.gymbro.data.local.FoodLogRow
import hr.rostanic20.gymbro.data.local.FoodRow
import hr.rostanic20.gymbro.data.toDomain
import hr.rostanic20.gymbro.data.toRecipes
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.nutritionFor
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FoodRepositoryImpl(
    private val local: FoodLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : FoodRepository {

    override fun foods(): Flow<List<Food>> =
        local.foods().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged().flowOn(dispatchers.io)

    override fun recipes(): Flow<List<Recipe>> =
        local.recipeRows().map { it.toRecipes() }.distinctUntilChanged().flowOn(dispatchers.io)

    override fun log(date: LocalDate): Flow<List<FoodLogEntry>> =
        local.logForDate(date.toEpochDay()).map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override suspend fun food(id: Long): Food? = local.foodById(id)?.toDomain()

    override suspend fun saveFood(draft: FoodDraft): Long {
        val row = FoodRow(
            name = draft.name.trim(),
            kcalPer100g = draft.per100g.kcal,
            proteinPer100g = draft.per100g.proteinG,
            carbsPer100g = draft.per100g.carbsG,
            fatPer100g = draft.per100g.fatG,
            unitName = draft.unitName?.trim()?.ifEmpty { null },
            unitGrams = draft.unitGrams,
        )
        val id = draft.id ?: return local.insertFood(row)
        local.updateFood(id, row)
        return id
    }

    override suspend fun setFavourite(foodId: Long, favourite: Boolean) {
        local.setFavourite(foodId, favourite)
    }

    override suspend fun logFood(date: LocalDate, meal: Meal, food: Food, grams: Double, nowMillis: Long) {
        local.insertLogs(listOf(logRow(date, meal, food.id, food.name, grams, food.nutritionFor(grams), nowMillis)))
    }

    override suspend fun logRecipe(date: LocalDate, meal: Meal, recipe: Recipe, nowMillis: Long) {
        local.insertLogs(
            recipe.items.map { item ->
                logRow(date, meal, item.food.id, item.food.name, item.grams, item.food.nutritionFor(item.grams), nowMillis)
            },
        )
    }

    override suspend fun logEstimate(date: LocalDate, meal: Meal, name: String, nutrition: Nutrition, nowMillis: Long) {
        local.insertLogs(listOf(logRow(date, meal, foodId = null, name, grams = null, nutrition, nowMillis)))
    }

    override suspend fun repeatMeal(from: LocalDate, to: LocalDate, meal: Meal, nowMillis: Long): Int {
        val previous = local.logForMeal(from.toEpochDay(), meal.slot.toLong())
        local.insertLogs(
            previous.map {
                FoodLogRow(
                    epochDay = to.toEpochDay(),
                    mealSlot = it.meal_slot,
                    foodId = it.food_id,
                    name = it.name,
                    grams = it.grams,
                    kcal = it.kcal,
                    proteinG = it.protein_g,
                    carbsG = it.carbs_g,
                    fatG = it.fat_g,
                    loggedAt = nowMillis,
                )
            },
        )
        return previous.size
    }

    override suspend fun deleteEntry(entryId: Long) {
        local.deleteLog(entryId)
    }

    private fun logRow(
        date: LocalDate,
        meal: Meal,
        foodId: Long?,
        name: String,
        grams: Double?,
        nutrition: Nutrition,
        nowMillis: Long,
    ): FoodLogRow = FoodLogRow(
        epochDay = date.toEpochDay(),
        mealSlot = meal.slot.toLong(),
        foodId = foodId,
        name = name,
        grams = grams,
        kcal = nutrition.kcal,
        proteinG = nutrition.proteinG,
        carbsG = nutrition.carbsG,
        fatG = nutrition.fatG,
        loggedAt = nowMillis,
    )
}
