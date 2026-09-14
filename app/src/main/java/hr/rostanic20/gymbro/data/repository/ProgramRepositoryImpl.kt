package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSource
import hr.rostanic20.gymbro.data.toWorkoutDays
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class ProgramRepositoryImpl(
    private val local: ProgramLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : ProgramRepository {

    override fun workoutDays(): Flow<List<WorkoutDay>> =
        combine(local.program(), local.alternatives()) { rows, alternatives -> rows.toWorkoutDays(alternatives) }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?) {
        local.updateLoadSettings(exerciseId, startLoadKg, incrementKg)
    }
}
