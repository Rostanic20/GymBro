package hr.rostanic20.gymbro.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.domain.model.CanteenPlate
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodLogEntry
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.domain.repository.MealSettingsRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import java.time.LocalTime

data class MealUiState(
    val date: LocalDate,
    val meal: Meal,
    val time: LocalTime,
    val entries: List<FoodLogEntry>,
    val recipes: List<Recipe>,
    val foods: List<Food>,
    val yesterdayEntries: Int,
)

class MealViewModel(
    epochDay: Long,
    slot: Int,
    private val foodRepository: FoodRepository,
    mealSettingsRepository: MealSettingsRepository,
    private val clock: WallClock,
) : ViewModel() {

    private val date = LocalDate.ofEpochDay(epochDay)
    private val meal = Meal.fromSlot(slot)

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val state: StateFlow<MealUiState?> =
        combine(
            foodRepository.log(date),
            foodRepository.log(date.minusDays(1)),
            foodRepository.recipes(),
            foodRepository.foods(),
            mealSettingsRepository.settings(),
        ) { today, yesterday, recipes, foods, mealSettings ->
            MealUiState(
                date = date,
                meal = meal,
                time = mealSettings.timeFor(meal),
                entries = today.filter { it.meal == meal },
                recipes = recipes.sortedByDescending { it.id == meal.suggestedRecipeId },
                foods = foods,
                yesterdayEntries = yesterday.count { it.meal == meal },
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun logRecipe(recipe: Recipe) {
        launchReporting(_messages) { foodRepository.logRecipe(date, meal, recipe, clock.nowMillis()) }
    }

    fun logFood(food: Food, grams: Double) {
        launchReporting(_messages) { foodRepository.logFood(date, meal, food, grams, clock.nowMillis()) }
    }

    fun logCanteen(plate: CanteenPlate, name: String) {
        launchReporting(_messages) { foodRepository.logEstimate(date, meal, name, plate.nutrition, clock.nowMillis()) }
    }

    fun repeatYesterday() {
        launchReporting(_messages) { foodRepository.repeatMeal(date.minusDays(1), date, meal, clock.nowMillis()) }
    }

    fun deleteEntry(entryId: Long) {
        launchReporting(_messages) { foodRepository.deleteEntry(entryId) }
    }
}
