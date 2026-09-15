package hr.rostanic20.gymbro.ui.meal

import hr.rostanic20.gymbro.domain.model.CanteenPlate
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.model.RecipeItem
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeClock
import hr.rostanic20.gymbro.util.FakeFoodRepository
import hr.rostanic20.gymbro.util.FakeMealSettingsRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MealViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val oats = Food(2, "Oats", Nutrition(379.0, 13.2, 67.7, 6.5), null, null, isFavourite = false)
    private val egg = Food(7, "Egg", Nutrition(143.0, 12.6, 0.7, 9.5), "egg", 55.0, isFavourite = false)
    private val dinner = Recipe(5, "Dinner: eggs and toast", listOf(RecipeItem(egg, 165.0)))
    private val shake = Recipe(1, "Shake #1", listOf(RecipeItem(oats, 40.0)))
    private val foods = FakeFoodRepository(foods = listOf(oats, egg), recipes = listOf(dinner, shake))
    private val clock = FakeClock(1_000)

    private fun TestScope.open(meal: Meal): MealViewModel {
        val viewModel = MealViewModel(monday.toEpochDay(), meal.slot, foods, FakeMealSettingsRepository(), clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun `the meal's planned recipe is listed first`() = runTest {
        assertEquals(listOf(1L, 5L), open(Meal.BREAKFAST_SHAKE).state.value!!.recipes.map { it.id })
        assertEquals(listOf(5L, 1L), open(Meal.DINNER).state.value!!.recipes.map { it.id })
    }

    @Test
    fun `logging a recipe adds its items to this meal only`() = runTest {
        val viewModel = open(Meal.BREAKFAST_SHAKE)

        viewModel.logRecipe(shake)
        advanceUntilIdle()

        val entries = viewModel.state.value!!.entries
        assertEquals(listOf("Oats"), entries.map { it.name })
        assertTrue(entries.all { it.meal == Meal.BREAKFAST_SHAKE && it.date == monday })
    }

    @Test
    fun `a food is logged by grams`() = runTest {
        val viewModel = open(Meal.DINNER)

        viewModel.logFood(egg, 110.0)
        advanceUntilIdle()

        val entry = viewModel.state.value!!.entries.single()
        assertEquals(110.0, entry.grams)
        assertEquals(157.3, entry.nutrition.kcal, 0.001)
    }

    @Test
    fun `a canteen plate is logged as an estimate`() = runTest {
        val viewModel = open(Meal.LUNCH)

        viewModel.logCanteen(CanteenPlate.PASTA_LITTLE_MEAT, "Pasta or rice, little meat")
        advanceUntilIdle()

        val entry = viewModel.state.value!!.entries.single()
        assertEquals("Pasta or rice, little meat", entry.name)
        assertNull(entry.foodId)
        assertEquals(10.0, entry.nutrition.proteinG, 0.001)
    }

    @Test
    fun `repeat yesterday is offered when yesterday had this meal and copies it`() = runTest {
        foods.logFood(monday.minusDays(1), Meal.BREAKFAST_SHAKE, oats, 40.0, nowMillis = 1)
        foods.logFood(monday.minusDays(1), Meal.DINNER, egg, 165.0, nowMillis = 1)
        val viewModel = open(Meal.BREAKFAST_SHAKE)
        assertEquals(1, viewModel.state.value!!.yesterdayEntries)

        viewModel.repeatYesterday()
        advanceUntilIdle()

        assertEquals(listOf("Oats"), viewModel.state.value!!.entries.map { it.name })
    }

    @Test
    fun `a deleted entry disappears`() = runTest {
        val viewModel = open(Meal.BREAKFAST_SHAKE)
        viewModel.logRecipe(shake)
        advanceUntilIdle()

        viewModel.deleteEntry(viewModel.state.value!!.entries.single().id)
        advanceUntilIdle()

        assertTrue(viewModel.state.value!!.entries.isEmpty())
    }

    @Test
    fun `a failed log is reported`() = runTest {
        foods.failWrites = true
        val viewModel = open(Meal.BREAKFAST_SHAKE)

        viewModel.logRecipe(shake)

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
    }
}
