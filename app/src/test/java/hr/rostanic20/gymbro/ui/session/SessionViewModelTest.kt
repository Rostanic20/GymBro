package hr.rostanic20.gymbro.ui.session

import hr.rostanic20.gymbro.domain.Suggestion
import hr.rostanic20.gymbro.domain.model.Alternative
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeClock
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.FakeProgramRepository
import hr.rostanic20.gymbro.util.FakeRestTimer
import hr.rostanic20.gymbro.util.FakeSessionRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import hr.rostanic20.gymbro.util.defaultProfile
import hr.rostanic20.gymbro.util.plannedExercise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val inclineRow = plannedExercise(29, "Barbell row, chest on incline bench")
    private val bench = plannedExercise(1, "Barbell bench press", startLoadKg = 40.0).copy(position = 1, isTop = true)
    private val row = plannedExercise(2, "Chest-supported row", startLoadKg = 30.0).copy(
        position = 2,
        alternatives = listOf(Alternative(inclineRow.exercise, "No cost.")),
    )
    private val upperA = WorkoutDay(1, "Upper A", "press emphasis", DayOfWeek.MONDAY, listOf(bench, row))
    private val profiles = FakeProfileRepository(defaultProfile.copy(programStart = monday.minusWeeks(2)))
    private val sessions = FakeSessionRepository()
    private val timer = FakeRestTimer()
    private val clock = FakeClock(1_000)

    private fun history(loadKg: Double, vararg reps: Int): List<LoggedSet> =
        reps.map { LoggedSet(0, 1, 1, loadKg, it, 2) }

    private suspend fun TestScope.openSession(isDeload: Boolean = false): SessionViewModel {
        val id = sessions.startSession(dayId = 1, date = monday, isDeload = isDeload, nowMillis = 500)
        val viewModel = SessionViewModel(id, sessions, FakeProgramRepository(listOf(upperA)), profiles, timer, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    private fun SessionViewModel.slot(position: Int): SlotState =
        state.value!!.slots.single { it.planned.position == position }

    @Test
    fun `hitting the top last time suggests the next weight`() = runTest {
        sessions.lastSetsByExercise[1] = history(40.0, 8, 8, 8)

        val viewModel = openSession()

        assertEquals(Suggestion.Increase(42.5), viewModel.slot(1).suggestion)
        assertEquals(Suggestion.FirstTime(30.0), viewModel.slot(2).suggestion)
    }

    @Test
    fun `logging a set stores it and starts the rest timer`() = runTest {
        val viewModel = openSession()

        viewModel.logSet(viewModel.slot(1), SetValues(42.5, 5, 2))
        advanceUntilIdle()

        assertEquals(listOf(5), viewModel.slot(1).sets.map { it.reps })
        assertEquals(181_000L, sessions.allSessions.single().restEndsAtMillis)
        assertEquals(181_000L to "Barbell bench press", timer.scheduled)
    }

    @Test
    fun `extending rest adds thirty seconds to whatever is left`() = runTest {
        val viewModel = openSession()
        viewModel.logSet(viewModel.slot(1), SetValues(42.5, 5, 2))
        advanceUntilIdle()

        clock.now = 100_000
        viewModel.extendRest()
        advanceUntilIdle()

        assertEquals(211_000L, sessions.allSessions.single().restEndsAtMillis)
        assertEquals(211_000L, timer.scheduled?.first)
    }

    @Test
    fun `skipping rest clears it and cancels the alarm`() = runTest {
        val viewModel = openSession()
        viewModel.logSet(viewModel.slot(1), SetValues(42.5, 5, 2))
        advanceUntilIdle()

        viewModel.skipRest()
        advanceUntilIdle()

        assertNull(sessions.allSessions.single().restEndsAtMillis)
        assertNull(timer.scheduled)
    }

    @Test
    fun `an alternative can be swapped in only until a set is logged`() = runTest {
        val viewModel = openSession()

        viewModel.swap(slotPosition = 2, exerciseId = 29)
        assertEquals("Barbell row, chest on incline bench", viewModel.slot(2).exercise.name)
        assertTrue(viewModel.slot(2).canSwap)

        viewModel.logSet(viewModel.slot(2), SetValues(35.0, 10, 1))
        advanceUntilIdle()
        viewModel.swap(slotPosition = 2, exerciseId = 2)

        assertEquals(29L, viewModel.slot(2).exercise.id)
        assertFalse(viewModel.slot(2).canSwap)
        assertEquals(29L, viewModel.slot(2).sets.single().exerciseId)
    }

    @Test
    fun `finishing saves the note, cancels the timer and then closes`() = runTest {
        val viewModel = openSession()

        viewModel.finish("Rack was taken")

        assertEquals(SessionEvent.Closed, viewModel.events.first())
        val session = sessions.allSessions.single()
        assertEquals(1_000L, session.finishedAtMillis)
        assertEquals("Rack was taken", session.note)
        assertEquals(1, timer.cancelCount)
    }

    @Test
    fun `discarding deletes the session and closes`() = runTest {
        val viewModel = openSession()
        viewModel.logSet(viewModel.slot(1), SetValues(40.0, 8, 2))
        advanceUntilIdle()

        viewModel.discard()

        assertEquals(SessionEvent.Closed, viewModel.events.first())
        assertTrue(sessions.allSessions.isEmpty())
    }

    @Test
    fun `a failed log is reported and starts no timer`() = runTest {
        val viewModel = openSession()
        sessions.failWrites = true

        viewModel.logSet(viewModel.slot(1), SetValues(40.0, 8, 2))

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
        assertNull(timer.scheduled)
    }

    @Test
    fun `a stalled top set advises a deload`() = runTest {
        sessions.topSetsByExercise[1] = listOf(TopSet(60.0, 5), TopSet(60.0, 6), TopSet(60.0, 7))

        val viewModel = openSession()

        assertTrue(viewModel.state.value!!.deloadAdvised)
    }

    @Test
    fun `a deload session halves the sets and keeps the weight`() = runTest {
        sessions.lastSetsByExercise[1] = history(40.0, 8, 8, 8)

        val viewModel = openSession(isDeload = true)

        assertEquals(2, viewModel.slot(1).planned.sets)
        assertEquals(Suggestion.Deload(40.0), viewModel.slot(1).suggestion)
    }
}
