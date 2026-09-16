package hr.rostanic20.gymbro.ui.widget

import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.nextMealOfDay
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.domain.repository.MealSettingsRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.total
import hr.rostanic20.gymbro.domain.weekOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

data class TodayWidgetData(
    val kcalEaten: Int,
    val kcalTarget: Int,
    val proteinEaten: Int,
    val proteinTarget: Int,
    val nextMeal: Meal?,
    val nextMealTime: LocalTime?,
    val hasPlannedRecipe: Boolean,
    val alreadyLogged: Boolean,
) {
    val kcalLeft: Int get() = kcalTarget - kcalEaten
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayWidgetLoader(
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val mealSettingsRepository: MealSettingsRepository,
    private val dates: DateProvider,
) {

    fun data(): Flow<TodayWidgetData> =
        combine(
            profileRepository.profile(),
            dates.todayFlow().flatMapLatest { foodRepository.log(it) },
            mealSettingsRepository.settings(),
            foodRepository.recipes(),
        ) { profile, log, settings, recipes ->
            val today = dates.today()
            val targets = profile.nutritionTargets(profile.weekOn(today))
            val eaten = log.total()
            val next = nextMeal(settings, log.map { it.meal }.toSet())
            TodayWidgetData(
                kcalEaten = eaten.kcal.roundToInt(),
                kcalTarget = targets.kcal,
                proteinEaten = eaten.proteinG.roundToInt(),
                proteinTarget = targets.proteinG,
                nextMeal = next,
                nextMealTime = next?.let(settings::timeFor),
                hasPlannedRecipe = next != null && recipes.any { it.id == next.suggestedRecipeId },
                alreadyLogged = next != null && log.any { it.meal == next },
            )
        }

    suspend fun logPlanned() {
        val settings = mealSettingsRepository.settings().first()
        val today = dates.today()
        val logged = foodRepository.log(today).first().map { it.meal }.toSet()
        val meal = nextMeal(settings, logged) ?: return
        if (meal in logged) return
        val recipe = plannedRecipe(meal) ?: return
        foodRepository.logRecipe(today, meal, recipe, dates.now().toInstant().toEpochMilli())
    }

    private suspend fun plannedRecipe(meal: Meal): Recipe? =
        meal.suggestedRecipeId?.let { id -> foodRepository.recipes().first().firstOrNull { it.id == id } }

    // The meal you still have to eat is more useful than the one you just logged.
    private fun nextMeal(settings: MealSettings, logged: Set<Meal>): Meal? {
        val now = dates.now().toLocalDateTime()
        return settings.activeMeals.firstOrNull { settings.timeFor(it) >= now.toLocalTime() && it !in logged }
            ?: nextMealOfDay(now, settings)
    }

    private fun today(): LocalDate = dates.today()
}
