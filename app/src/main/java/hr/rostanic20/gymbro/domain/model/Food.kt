package hr.rostanic20.gymbro.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Nutrition(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    operator fun plus(other: Nutrition): Nutrition = Nutrition(
        kcal = kcal + other.kcal,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
    )

    companion object {
        val ZERO = Nutrition(0.0, 0.0, 0.0, 0.0)
    }
}

data class Food(
    val id: Long,
    val name: String,
    val per100g: Nutrition,
    val unitName: String?,
    val unitGrams: Double?,
    val isFavourite: Boolean,
)

data class FoodDraft(
    val id: Long?,
    val name: String,
    val per100g: Nutrition,
    val unitName: String?,
    val unitGrams: Double?,
)

data class RecipeItem(
    val food: Food,
    val grams: Double,
)

data class Recipe(
    val id: Long,
    val name: String,
    val items: List<RecipeItem>,
)

data class FoodLogEntry(
    val id: Long,
    val date: LocalDate,
    val meal: Meal,
    val foodId: Long?,
    val name: String,
    val grams: Double?,
    val nutrition: Nutrition,
)

enum class Meal(
    val slot: Int,
    val time: LocalTime,
    val onWeekends: Boolean,
    val suggestedRecipeId: Long?,
) {
    BREAKFAST_SHAKE(0, LocalTime.of(7, 0), onWeekends = true, suggestedRecipeId = 1),
    DESK_SNACK(1, LocalTime.of(10, 0), onWeekends = false, suggestedRecipeId = 7),
    LUNCH(2, LocalTime.of(12, 0), onWeekends = false, suggestedRecipeId = null),
    PRE_GYM(3, LocalTime.of(16, 0), onWeekends = false, suggestedRecipeId = 6),
    POST_GYM_SHAKE(4, LocalTime.of(18, 45), onWeekends = true, suggestedRecipeId = 2),
    DINNER(5, LocalTime.of(20, 30), onWeekends = false, suggestedRecipeId = 5),
    ;

    companion object {
        fun fromSlot(slot: Int): Meal = entries.first { it.slot == slot }
    }
}

enum class CanteenPlate(val nutrition: Nutrition) {
    MEAT_OR_FISH(Nutrition(kcal = 650.0, proteinG = 28.0, carbsG = 70.0, fatG = 22.0)),
    STEW(Nutrition(kcal = 650.0, proteinG = 22.0, carbsG = 70.0, fatG = 25.0)),
    PASTA_LITTLE_MEAT(Nutrition(kcal = 650.0, proteinG = 10.0, carbsG = 95.0, fatG = 20.0)),
    SOUP_NO_MEAT(Nutrition(kcal = 600.0, proteinG = 8.0, carbsG = 90.0, fatG = 18.0)),
}
