package hr.rostanic20.gymbro.domain.repository

import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SessionRepository {
    fun activeSession(): Flow<WorkoutSession?>
    fun session(id: Long): Flow<WorkoutSession?>
    fun sessionsOn(date: LocalDate): Flow<List<WorkoutSession>>
    fun sets(sessionId: Long): Flow<List<LoggedSet>>
    suspend fun lastSets(exerciseId: Long, excludedSessionId: Long): List<LoggedSet>
    suspend fun recentTopSets(exerciseId: Long, limit: Int): List<TopSet>
    suspend fun startSession(dayId: Long, date: LocalDate, isDeload: Boolean, nowMillis: Long): Long
    suspend fun logSet(sessionId: Long, exerciseId: Long, slotPosition: Int, values: SetValues, nowMillis: Long): Long
    suspend fun updateSet(setId: Long, values: SetValues)
    suspend fun deleteSet(setId: Long)
    suspend fun setNote(sessionId: Long, note: String)
    suspend fun setRestEndsAt(sessionId: Long, restEndsAtMillis: Long?)
    suspend fun finishSession(sessionId: Long, nowMillis: Long)
    suspend fun discardSession(sessionId: Long)
}
