package hr.rostanic20.gymbro.data.repository

import android.util.Log
import hr.rostanic20.gymbro.data.local.MealSettingsLocalDataSource
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import hr.rostanic20.gymbro.domain.repository.MealSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeParseException

class MealSettingsRepositoryImpl(
    private val local: MealSettingsLocalDataSource,
) : MealSettingsRepository {

    override fun settings(): Flow<MealSettings> =
        local.profile().map { profile ->
            MealSettings(
                times = profile.mealTimes.mapNotNull { (slot, text) -> meal(slot)?.let { it to parse(text) } }
                    .filter { it.second != null }
                    .associate { (meal, time) -> meal to requireNotNull(time) },
                disabled = profile.disabledMeals.mapNotNull(::meal).toSet(),
            )
        }.distinctUntilChanged()

    override suspend fun setTime(meal: Meal, time: LocalTime) {
        local.setTime(meal.slot, time.toString())
    }

    override suspend fun setEnabled(meal: Meal, enabled: Boolean) {
        local.setEnabled(meal.slot, enabled)
    }

    private fun meal(slot: Int): Meal? = Meal.entries.firstOrNull { it.slot == slot }

    private fun parse(text: String): LocalTime? =
        try {
            LocalTime.parse(text)
        } catch (e: DateTimeParseException) {
            Log.w("GymBro", "Ignoring unreadable meal time $text", e)
            null
        }
}
