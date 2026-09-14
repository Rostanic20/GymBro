package hr.rostanic20.gymbro.ui.foods

import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.util.FakeFoodRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val oats = Food(2, "Oats", Nutrition(379.0, 13.2, 67.7, 6.5), null, null, isFavourite = false)
    private val foods = FakeFoodRepository(foods = listOf(oats))

    private fun TestScope.open(foodId: Long?): FoodEditViewModel {
        val viewModel = FoodEditViewModel(foodId, foods)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun `an existing food is loaded for editing`() = runTest {
        val state = open(foodId = 2).state.value

        assertTrue(state.loaded)
        assertEquals(oats, state.food)
    }

    @Test
    fun `a new food starts empty`() = runTest {
        val state = open(foodId = null).state.value

        assertTrue(state.loaded)
        assertNull(state.food)
    }

    @Test
    fun `saving stores the food and then reports it saved`() = runTest {
        val viewModel = open(foodId = null)

        viewModel.save(FoodDraft(null, "Greek yogurt", Nutrition(97.0, 9.0, 4.0, 5.0), null, null))

        assertEquals(FoodEditEvent.Saved, viewModel.events.first())
        assertEquals("Greek yogurt", foods.foods().first().last().name)
    }

    @Test
    fun `the food list toggles a favourite`() = runTest {
        val list = FoodsViewModel(foods)

        list.toggleFavourite(oats)

        assertTrue(foods.food(2)!!.isFavourite)
    }
}
