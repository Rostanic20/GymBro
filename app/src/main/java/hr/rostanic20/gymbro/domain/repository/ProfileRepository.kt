package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ProfileRepository {
    fun profile(): Flow<Profile>
    suspend fun setProgramStart(date: LocalDate?)
    suspend fun setTargets(maintenanceKcal: Int, surplusKcal: Int, proteinG: Int, fatG: Int)
    suspend fun setMealReminders(enabled: Boolean)
}
