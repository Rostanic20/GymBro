package hr.rostanic20.gymbro.data.repository

import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.local.SessionLocalDataSource
import hr.rostanic20.gymbro.data.toDomain
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.SessionSummary
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.domain.model.TopSetPoint
import hr.rostanic20.gymbro.domain.model.WorkoutSession
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SessionRepositoryImpl(
    private val local: SessionLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : SessionRepository {

    override fun activeSession(): Flow<WorkoutSession?> =
        local.active().map { it?.toDomain() }.distinctUntilChanged().flowOn(dispatchers.io)

    override fun session(id: Long): Flow<WorkoutSession?> =
        local.byId(id).map { it?.toDomain() }.distinctUntilChanged().flowOn(dispatchers.io)

    override fun sessionsOn(date: LocalDate): Flow<List<WorkoutSession>> =
        local.onDate(date.toEpochDay()).map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override fun sets(sessionId: Long): Flow<List<LoggedSet>> =
        local.sets(sessionId).map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override fun topSetHistory(exerciseId: Long, limit: Int): Flow<List<TopSetPoint>> =
        local.topSetHistory(exerciseId, limit.toLong())
            .map { rows ->
                rows.reversed().map { TopSetPoint(LocalDate.ofEpochDay(it.date_epoch_day), it.load_kg, it.reps.toInt()) }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override fun recentSessions(limit: Int): Flow<List<SessionSummary>> =
        local.finishedSessions(limit.toLong())
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)

    override suspend fun lastSets(exerciseId: Long, excludedSessionId: Long): List<LoggedSet> =
        local.lastSets(exerciseId, excludedSessionId).map { it.toDomain() }

    override suspend fun recentTopSets(exerciseId: Long, limit: Int): List<TopSet> =
        local.firstSets(exerciseId, limit.toLong()).map { it.toDomain() }

    override suspend fun startSession(dayId: Long, date: LocalDate, isDeload: Boolean, nowMillis: Long): Long =
        local.insertSession(dayId, date.toEpochDay(), nowMillis, isDeload)

    override suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        slotPosition: Int,
        values: SetValues,
        nowMillis: Long,
    ): Long = local.insertSet(
        sessionId = sessionId,
        exerciseId = exerciseId,
        slotPosition = slotPosition.toLong(),
        loadKg = values.loadKg,
        reps = values.reps.toLong(),
        rir = values.rir?.toLong(),
        loggedAt = nowMillis,
    )

    override suspend fun updateSet(setId: Long, values: SetValues) {
        local.updateSet(setId, values.loadKg, values.reps.toLong(), values.rir?.toLong())
    }

    override suspend fun deleteSet(setId: Long) {
        local.deleteSet(setId)
    }

    override suspend fun setNote(sessionId: Long, note: String) {
        local.updateNote(sessionId, note.trim().ifEmpty { null })
    }

    override suspend fun setRestEndsAt(sessionId: Long, restEndsAtMillis: Long?) {
        local.updateRestEndsAt(sessionId, restEndsAtMillis)
    }

    override suspend fun finishSession(sessionId: Long, nowMillis: Long) {
        local.finish(sessionId, nowMillis)
    }

    override suspend fun discardSession(sessionId: Long) {
        local.delete(sessionId)
    }
}
