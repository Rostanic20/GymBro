package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Recipe

private const val GRAMS_PER_REFERENCE = 100.0

fun Food.nutritionFor(grams: Double): Nutrition {
    val factor = grams / GRAMS_PER_REFERENCE
    return Nutrition(
        kcal = per100g.kcal * factor,
        proteinG = per100g.proteinG * factor,
        carbsG = per100g.carbsG * factor,
        fatG = per100g.fatG * factor,
    )
}

val Recipe.nutrition: Nutrition
    get() = items.fold(Nutrition.ZERO) { total, item -> total + item.food.nutritionFor(item.grams) }

fun List<FoodLogEntry>.total(): Nutrition = fold(Nutrition.ZERO) { total, entry -> total + entry.nutrition }
