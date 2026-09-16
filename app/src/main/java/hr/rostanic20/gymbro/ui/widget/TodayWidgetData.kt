package hr.rostanic20.gymbro.ui.widget

import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Recipe
import hr.rostanic20.gymbro.domain.nextMealOfDay
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.domain.repository.MealSettingsRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.total
import hr.rostanic20.gymbro.domain.weekOn
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import kotlin.math.roundToInt

data class TodayWidgetData(
    val kcalEaten: Int,
    val kcalTarget: Int,
    val proteinEaten: Int,
    val proteinTarget: Int,
    val nextMeal: Meal?,
    val nextMealTime: LocalTime?,
    val plannedRecipe: Recipe?,
    val alreadyLogged: Boolean,
) {
    val kcalLeft: Int get() = kcalTarget - kcalEaten
}

class TodayWidgetLoader(
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val mealSettingsRepository: MealSettingsRepository,
    private val dates: DateProvider,
) {

    suspend fun load(): TodayWidgetData {
        val today = dates.today()
        val profile = profileRepository.profile().first()
        val targets = profile.nutritionTargets(profile.weekOn(today))
        val log = foodRepository.log(today).first()
        val settings = mealSettingsRepository.settings().first()
        val eaten = log.total()
        val next = nextMealOfDay(dates.now().toLocalDateTime(), settings)
        val planned = next?.suggestedRecipeId?.let { id -> foodRepository.recipes().first().firstOrNull { it.id == id } }
        return TodayWidgetData(
            kcalEaten = eaten.kcal.roundToInt(),
            kcalTarget = targets.kcal,
            proteinEaten = eaten.proteinG.roundToInt(),
            proteinTarget = targets.proteinG,
            nextMeal = next,
            nextMealTime = next?.let(settings::timeFor),
            plannedRecipe = planned,
            alreadyLogged = next != null && log.any { it.meal == next },
        )
    }

    suspend fun logPlanned() {
        val data = load()
        val meal = data.nextMeal ?: return
        val recipe = data.plannedRecipe ?: return
        foodRepository.logRecipe(dates.today(), meal, recipe, dates.now().toInstant().toEpochMilli())
    }
}
