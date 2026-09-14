package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.WorkoutDay
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun workoutDays(): Flow<List<WorkoutDay>>
    suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?)
}
