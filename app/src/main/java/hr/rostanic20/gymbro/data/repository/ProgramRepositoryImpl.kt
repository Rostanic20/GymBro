package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSource
import hr.rostanic20.gymbro.data.toWorkoutDays
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ProgramRepositoryImpl(
    private val local: ProgramLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : ProgramRepository {

    override fun workoutDays(): Flow<List<WorkoutDay>> =
        local.program()
            .map { it.toWorkoutDays() }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
}
