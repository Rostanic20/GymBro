package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.AlternativeRowEntity
import hr.rostanic20.gymbro.data.ExerciseEntity
import hr.rostanic20.gymbro.data.ProgramRowEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ProgramLocalDataSource {
    fun program(): Flow<List<ProgramRowEntity>>
    fun alternatives(): Flow<List<AlternativeRowEntity>>
    fun exercises(): Flow<List<ExerciseEntity>>
    suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?)
    suspend fun updatePrescription(dayId: Long, position: Long, sets: Long, repMin: Long, repMax: Long)
    suspend fun updateHidden(dayId: Long, position: Long, hidden: Boolean)
    suspend fun addExercise(dayId: Long, exerciseId: Long, sets: Long, repMin: Long, repMax: Long)
}

class ProgramLocalDataSourceImpl(
    private val db: AppDb,
    private val dispatchers: DispatcherProvider,
) : ProgramLocalDataSource {

    private val query = db.programQueries
    private val writeContext = dispatchers.io + NonCancellable

    override fun program(): Flow<List<ProgramRowEntity>> =
        query.selectProgram().asFlow().map { it.awaitAsList() }

    override fun alternatives(): Flow<List<AlternativeRowEntity>> =
        query.selectAlternatives().asFlow().map { it.awaitAsList() }

    override fun exercises(): Flow<List<ExerciseEntity>> =
        query.selectExercises().asFlow().map { it.awaitAsList() }

    override suspend fun updateLoadSettings(exerciseId: Long, startLoadKg: Double?, incrementKg: Double?): Unit =
        withContext(writeContext) {
            query.updateLoadSettings(startLoadKg = startLoadKg, incrementKg = incrementKg, id = exerciseId)
        }

    override suspend fun updatePrescription(
        dayId: Long,
        position: Long,
        sets: Long,
        repMin: Long,
        repMax: Long,
    ): Unit = withContext(writeContext) {
        query.updatePrescription(sets = sets, repMin = repMin, repMax = repMax, dayId = dayId, position = position)
    }

    override suspend fun updateHidden(dayId: Long, position: Long, hidden: Boolean): Unit = withContext(writeContext) {
        query.updateHidden(hidden = if (hidden) 1L else 0L, dayId = dayId, position = position)
    }

    override suspend fun addExercise(dayId: Long, exerciseId: Long, sets: Long, repMin: Long, repMax: Long): Unit =
        withContext(writeContext) {
            db.transaction {
                val position = query.nextPosition(dayId).awaitAsOne()
                query.insertDayExercise(
                    dayId = dayId,
                    position = position,
                    exerciseId = exerciseId,
                    sets = sets,
                    repMin = repMin,
                    repMax = repMax,
                )
            }
        }
}
