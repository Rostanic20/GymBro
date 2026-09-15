package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.SessionLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.SessionRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.domain.model.TopSetPoint
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SessionRepositoryTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        AppDb.Schema.synchronous().create(it)
    }
    private val db = AppDb(driver)
    private val monday = LocalDate.of(2026, 9, 14)
    private val bench = 1L
    private val row = 2L

    @After
    fun tearDown() {
        driver.close()
    }

    private fun TestScope.repository(): SessionRepositoryImpl {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return SessionRepositoryImpl(SessionLocalDataSourceImpl(db, dispatchers), dispatchers)
    }

    private suspend fun SessionRepositoryImpl.logBench(sessionId: Long, loadKg: Double, vararg reps: Int) {
        reps.forEach { logSet(sessionId, bench, 1, SetValues(loadKg, it, 2), nowMillis = 0) }
    }

    @Test
    fun `a started session stays active until it is finished`() = runTest {
        val repository = repository()

        val id = repository.startSession(dayId = 1, date = monday, isDeload = false, nowMillis = 1_000)
        repository.setRestEndsAt(id, 90_000)

        assertEquals(id, repository.activeSession().first()?.id)
        assertEquals(90_000L, repository.session(id).first()?.restEndsAtMillis)

        repository.finishSession(id, nowMillis = 2_000)

        assertNull(repository.activeSession().first())
        val finished = repository.session(id).first()
        assertEquals(2_000L, finished?.finishedAtMillis)
        assertNull(finished?.restEndsAtMillis)
        assertEquals(listOf(id), repository.sessionsOn(monday).first().map { it.id })
    }

    @Test
    fun `logged sets come back in order and can be edited or removed`() = runTest {
        val repository = repository()
        val id = repository.startSession(1, monday, isDeload = false, nowMillis = 1_000)

        val first = repository.logSet(id, bench, 1, SetValues(40.0, 8, 2), nowMillis = 1_100)
        val second = repository.logSet(id, row, 2, SetValues(30.0, 10, null), nowMillis = 1_200)
        repository.updateSet(first, SetValues(42.5, 7, 1))
        repository.deleteSet(second)

        val sets = repository.sets(id).first()
        assertEquals(1, sets.size)
        assertEquals(42.5, sets.single().loadKg)
        assertEquals(7, sets.single().reps)
        assertEquals(1, sets.single().rir)
        assertEquals(1, sets.single().slotPosition)
    }

    @Test
    fun `last sets come from the latest finished non-deload session`() = runTest {
        val repository = repository()
        val normal = repository.startSession(1, monday, isDeload = false, nowMillis = 1_000)
        repository.logBench(normal, 40.0, 8, 8, 7)
        repository.finishSession(normal, 1_500)
        val deload = repository.startSession(1, monday.plusWeeks(7), isDeload = true, nowMillis = 2_000)
        repository.logBench(deload, 40.0, 5)
        repository.finishSession(deload, 2_500)
        val abandoned = repository.startSession(1, monday.plusWeeks(8), isDeload = false, nowMillis = 3_000)
        repository.logBench(abandoned, 42.5, 5)
        val current = repository.startSession(1, monday.plusWeeks(9), isDeload = false, nowMillis = 4_000)

        val last = repository.lastSets(bench, excludedSessionId = current)

        assertEquals(listOf(8, 8, 7), last.map { it.reps })
        assertTrue(repository.lastSets(row, excludedSessionId = current).isEmpty())
    }

    @Test
    fun `top set history is each finished session's first set, newest first`() = runTest {
        val repository = repository()
        listOf(7, 6, 5).forEachIndexed { index, reps ->
            val id = repository.startSession(1, monday.plusWeeks(index.toLong()), false, nowMillis = index * 1_000L)
            repository.logBench(id, 60.0, reps, reps + 3)
            repository.finishSession(id, index * 1_000L + 500)
        }

        assertEquals(
            listOf(TopSet(60.0, 5), TopSet(60.0, 6)),
            repository.recentTopSets(bench, limit = 2),
        )
    }

    @Test
    fun `chart history keeps deloads, skips unfinished sessions and runs oldest to newest`() = runTest {
        val repository = repository()
        listOf(false, true, false).forEachIndexed { index, deload ->
            val id = repository.startSession(1, monday.plusWeeks(index.toLong()), deload, nowMillis = index * 1_000L)
            repository.logBench(id, 60.0 + index, 8 - index)
            repository.finishSession(id, index * 1_000L + 500)
        }
        val open = repository.startSession(1, monday.plusWeeks(3), isDeload = false, nowMillis = 3_000)
        repository.logBench(open, 70.0, 3)

        assertEquals(
            listOf(TopSetPoint(monday.plusWeeks(1), 61.0, 7), TopSetPoint(monday.plusWeeks(2), 62.0, 6)),
            repository.topSetHistory(bench, limit = 2).first(),
        )
    }

    @Test
    fun `the history lists finished sessions newest first with their set counts`() = runTest {
        val repository = repository()
        val first = repository.startSession(1, monday, isDeload = false, nowMillis = 1_000)
        repository.logBench(first, 60.0, 8, 8, 7)
        repository.finishSession(first, 1_500)
        val second = repository.startSession(2, monday.plusDays(1), isDeload = true, nowMillis = 2_000)
        repository.logBench(second, 60.0, 5)
        repository.finishSession(second, 2_500)
        val open = repository.startSession(3, monday.plusDays(2), isDeload = false, nowMillis = 3_000)
        repository.logBench(open, 60.0, 5)

        val history = repository.recentSessions(limit = 10).first()

        assertEquals(listOf(second, first), history.map { it.id })
        assertEquals(listOf(1, 3), history.map { it.setCount })
        assertTrue(history.first().isDeload)
    }

    @Test
    fun `discarding removes the session and its sets`() = runTest {
        val repository = repository()
        val id = repository.startSession(1, monday, isDeload = false, nowMillis = 1_000)
        repository.logBench(id, 40.0, 8)

        repository.discardSession(id)

        assertNull(repository.session(id).first())
        assertTrue(repository.sets(id).first().isEmpty())
        assertNull(repository.activeSession().first())
    }
}
