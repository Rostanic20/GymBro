package hr.rostanic20.gymbro.domain.model

import java.time.LocalTime

data class MealSettings(
    val times: Map<Meal, LocalTime> = emptyMap(),
    val disabled: Set<Meal> = emptySet(),
) {
    fun timeFor(meal: Meal): LocalTime = times[meal] ?: meal.time

    fun isEnabled(meal: Meal): Boolean = meal !in disabled

    val activeMeals: List<Meal> get() = Meal.entries.filter(::isEnabled).sortedBy(::timeFor)

    val orderedMeals: List<Meal> get() = Meal.entries.sortedBy(::timeFor)
}
