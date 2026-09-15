package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun workoutDays(): Flow<List<WorkoutDay>>
    fun editableDays(): Flow<List<WorkoutDay>>
    fun exercises(): Flow<List<Exercise>>
    suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?)
    suspend fun updatePrescription(dayId: Long, position: Int, sets: Int, reps: IntRange)
    suspend fun setExerciseHidden(dayId: Long, position: Int, hidden: Boolean)
    suspend fun addExercise(dayId: Long, exerciseId: Long, sets: Int, reps: IntRange)
}
