package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface MealSettingsRepository {
    fun settings(): Flow<MealSettings>
    suspend fun setTime(meal: Meal, time: LocalTime)
    suspend fun setEnabled(meal: Meal, enabled: Boolean)
}
