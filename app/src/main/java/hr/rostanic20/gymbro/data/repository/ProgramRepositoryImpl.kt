package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSource
import hr.rostanic20.gymbro.data.toDomain
import hr.rostanic20.gymbro.data.toWorkoutDays
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ProgramRepositoryImpl(
    private val local: ProgramLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : ProgramRepository {

    override fun workoutDays(): Flow<List<WorkoutDay>> =
        editableDays().map { days ->
            days.map { day -> day.copy(exercises = day.exercises.filterNot { it.isHidden }) }
        }.distinctUntilChanged()

    override fun editableDays(): Flow<List<WorkoutDay>> =
        combine(local.program(), local.alternatives()) { rows, alternatives -> rows.toWorkoutDays(alternatives) }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override fun exercises(): Flow<List<Exercise>> =
        local.exercises()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?) {
        local.updateLoadSettings(exerciseId, startLoadKg, incrementKg)
    }

    override suspend fun updatePrescription(dayId: Long, position: Int, sets: Int, reps: IntRange) {
        local.updatePrescription(dayId, position.toLong(), sets.toLong(), reps.first.toLong(), reps.last.toLong())
    }

    override suspend fun setExerciseHidden(dayId: Long, position: Int, hidden: Boolean) {
        local.updateHidden(dayId, position.toLong(), hidden)
    }

    override suspend fun addExercise(dayId: Long, exerciseId: Long, sets: Int, reps: IntRange) {
        local.addExercise(dayId, exerciseId, sets.toLong(), reps.first.toLong(), reps.last.toLong())
    }
}
