package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.SessionEntity
import hr.rostanic20.gymbro.data.SetEntity
import hr.rostanic20.gymbro.data.TopSetRowEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface SessionLocalDataSource {
    fun active(): Flow<SessionEntity?>
    fun byId(id: Long): Flow<SessionEntity?>
    fun onDate(epochDay: Long): Flow<List<SessionEntity>>
    fun sets(sessionId: Long): Flow<List<SetEntity>>
    suspend fun lastSets(exerciseId: Long, excludedSessionId: Long): List<SetEntity>
    suspend fun firstSets(exerciseId: Long, limit: Long): List<TopSetRowEntity>
    suspend fun insertSession(dayId: Long, epochDay: Long, startedAt: Long, isDeload: Boolean): Long
    suspend fun insertSet(
        sessionId: Long,
        exerciseId: Long,
        slotPosition: Long,
        loadKg: Double?,
        reps: Long,
        rir: Long?,
        loggedAt: Long,
    ): Long
    suspend fun updateSet(id: Long, loadKg: Double?, reps: Long, rir: Long?)
    suspend fun deleteSet(id: Long)
    suspend fun updateNote(sessionId: Long, note: String?)
    suspend fun updateRestEndsAt(sessionId: Long, restEndsAt: Long?)
    suspend fun finish(sessionId: Long, finishedAt: Long)
    suspend fun delete(sessionId: Long)
}

class SessionLocalDataSourceImpl(
    private val db: AppDb,
    private val dispatchers: DispatcherProvider,
) : SessionLocalDataSource {

    private val query = db.sessionQueries
    private val writeContext = dispatchers.io + NonCancellable

    override fun active(): Flow<SessionEntity?> =
        query.selectActive().asFlow().map { it.awaitAsOneOrNull() }

    override fun byId(id: Long): Flow<SessionEntity?> =
        query.selectById(id).asFlow().map { it.awaitAsOneOrNull() }

    override fun onDate(epochDay: Long): Flow<List<SessionEntity>> =
        query.selectOnDate(epochDay).asFlow().map { it.awaitAsList() }

    override fun sets(sessionId: Long): Flow<List<SetEntity>> =
        query.selectSetsForSession(sessionId).asFlow().map { it.awaitAsList() }

    override suspend fun lastSets(exerciseId: Long, excludedSessionId: Long): List<SetEntity> =
        withContext(dispatchers.io) {
            val sessionId = query.selectLastSessionIdWithExercise(exerciseId, excludedSessionId).awaitAsOneOrNull()
                ?: return@withContext emptyList()
            query.selectSetsForSessionAndExercise(sessionId, exerciseId).awaitAsList()
        }

    override suspend fun firstSets(exerciseId: Long, limit: Long): List<TopSetRowEntity> =
        withContext(dispatchers.io) {
            query.selectFirstSetsForExercise(exerciseId, limit).awaitAsList()
        }

    override suspend fun insertSession(dayId: Long, epochDay: Long, startedAt: Long, isDeload: Boolean): Long =
        withContext(writeContext) {
            db.transactionWithResult {
                query.insertSession(dayId, epochDay, startedAt, if (isDeload) 1L else 0L)
                query.lastInsertRowId().awaitAsOne()
            }
        }

    override suspend fun insertSet(
        sessionId: Long,
        exerciseId: Long,
        slotPosition: Long,
        loadKg: Double?,
        reps: Long,
        rir: Long?,
        loggedAt: Long,
    ): Long = withContext(writeContext) {
        db.transactionWithResult {
            query.insertSet(sessionId, exerciseId, slotPosition, loadKg, reps, rir, loggedAt)
            query.lastInsertRowId().awaitAsOne()
        }
    }

    override suspend fun updateSet(id: Long, loadKg: Double?, reps: Long, rir: Long?): Unit =
        withContext(writeContext) {
            query.updateSet(loadKg = loadKg, reps = reps, rir = rir, id = id)
        }

    override suspend fun deleteSet(id: Long): Unit = withContext(writeContext) {
        query.deleteSet(id)
    }

    override suspend fun updateNote(sessionId: Long, note: String?): Unit = withContext(writeContext) {
        query.updateNote(note = note, id = sessionId)
    }

    override suspend fun updateRestEndsAt(sessionId: Long, restEndsAt: Long?): Unit = withContext(writeContext) {
        query.updateRestEndsAt(restEndsAt = restEndsAt, id = sessionId)
    }

    override suspend fun finish(sessionId: Long, finishedAt: Long): Unit = withContext(writeContext) {
        query.finish(finishedAt = finishedAt, id = sessionId)
    }

    override suspend fun delete(sessionId: Long): Unit = withContext(writeContext) {
        db.transaction {
            query.deleteSetsForSession(sessionId)
            query.deleteSession(sessionId)
        }
    }
}
