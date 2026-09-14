package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.model.RecipeItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class NutritionTest {

    private val oats = Food(2, "Oats", Nutrition(379.0, 13.2, 67.7, 6.5), null, null, isFavourite = false)
    private val whey = Food(4, "Whey protein", Nutrition(400.0, 78.0, 8.0, 6.0), "scoop", 25.0, isFavourite = false)

    @Test
    fun `nutrition scales with grams from the per-100 g values`() {
        val forty = oats.nutritionFor(40.0)

        assertEquals(151.6, forty.kcal, 0.001)
        assertEquals(5.28, forty.proteinG, 0.001)
        assertEquals(27.08, forty.carbsG, 0.001)
        assertEquals(2.6, forty.fatG, 0.001)
    }

    @Test
    fun `a recipe adds up its ingredients`() {
        val recipe = Recipe(1, "Oats and whey", listOf(RecipeItem(oats, 40.0), RecipeItem(whey, 25.0)))

        assertEquals(251.6, recipe.nutrition.kcal, 0.001)
        assertEquals(24.78, recipe.nutrition.proteinG, 0.001)
    }

    @Test
    fun `a day's total adds every entry`() {
        val date = LocalDate.of(2026, 9, 14)
        val entries = listOf(
            FoodLogEntry(1, date, Meal.BREAKFAST_SHAKE, 2, "Oats", 40.0, Nutrition(150.0, 5.0, 27.0, 3.0)),
            FoodLogEntry(2, date, Meal.LUNCH, null, "Canteen", null, Nutrition(650.0, 28.0, 70.0, 22.0)),
        )

        assertEquals(Nutrition(800.0, 33.0, 97.0, 25.0), entries.total())
        assertEquals(Nutrition.ZERO, emptyList<FoodLogEntry>().total())
    }
}
