package hr.rostanic20.gymbro.ui.workout

import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeClock
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.FakeProgramRepository
import hr.rostanic20.gymbro.util.FakeSessionRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import hr.rostanic20.gymbro.util.weekProgram
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val profiles = FakeProfileRepository()
    private val program = FakeProgramRepository(weekProgram)
    private val sessions = FakeSessionRepository()
    private val dates = FakeDateProvider(monday)
    private val clock = FakeClock(5_000)

    private fun viewModel(dayId: Long) = WorkoutViewModel(dayId, profiles, program, sessions, dates, clock)

    private fun TestScope.collect(viewModel: WorkoutViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `shows the requested day with deload sets in week eight`() = runTest {
        profiles.setProgramStart(monday)
        dates.date = monday.plusWeeks(7).plusDays(1)
        val viewModel = viewModel(dayId = 2)
        collect(viewModel)

        val state = viewModel.state.value
        assertEquals("Lower A", state?.day?.name)
        assertEquals(8, state?.week)
        assertEquals(2, state?.day?.exercises?.single()?.sets)
    }

    @Test
    fun `an unknown day shows nothing`() = runTest {
        val viewModel = viewModel(dayId = 99)
        collect(viewModel)

        assertNull(viewModel.state.value?.day)
    }

    @Test
    fun `starting a workout creates today's session and opens it`() = runTest {
        val viewModel = viewModel(dayId = 2)

        viewModel.startSession()

        val session = sessions.allSessions.single()
        assertEquals(WorkoutEvent.OpenSession(session.id), viewModel.events.first())
        assertEquals(2L, session.dayId)
        assertEquals(monday, session.date)
        assertEquals(5_000L, session.startedAtMillis)
        assertFalse(session.isDeload)
    }

    @Test
    fun `a workout started in week eight is a deload`() = runTest {
        profiles.setProgramStart(monday)
        dates.date = monday.plusWeeks(7)
        val viewModel = viewModel(dayId = 1)

        viewModel.startSession()
        viewModel.events.first()

        assertTrue(sessions.allSessions.single().isDeload)
    }

    @Test
    fun `an unfinished session for the same day is reopened, not duplicated`() = runTest {
        val existing = sessions.startSession(dayId = 2, date = monday, isDeload = false, nowMillis = 1_000)
        val viewModel = viewModel(dayId = 2)

        viewModel.startSession()

        assertEquals(WorkoutEvent.OpenSession(existing), viewModel.events.first())
        assertEquals(1, sessions.allSessions.size)
    }

    @Test
    fun `an unfinished session for another day blocks a second one`() = runTest {
        sessions.startSession(dayId = 1, date = monday, isDeload = false, nowMillis = 1_000)
        val viewModel = viewModel(dayId = 2)
        collect(viewModel)

        viewModel.startSession()
        advanceUntilIdle()

        assertEquals(1, sessions.allSessions.size)
        assertEquals("Upper A", viewModel.state.value?.activeSessionDayName)
    }

    @Test
    fun `a finished session today is marked as logged`() = runTest {
        val id = sessions.startSession(dayId = 2, date = monday, isDeload = false, nowMillis = 1_000)
        sessions.finishSession(id, nowMillis = 2_000)
        val viewModel = viewModel(dayId = 2)
        collect(viewModel)

        assertTrue(viewModel.state.value?.loggedToday == true)
        assertNull(viewModel.state.value?.activeSession)
    }

    @Test
    fun `saving weights passes the values through`() = runTest {
        viewModel(dayId = 2).updateLoadSettings(8, LoadSettings(startLoadKg = 45.0, incrementKg = 5.0))
        advanceUntilIdle()

        assertEquals(listOf(Triple(8L, 45.0, 5.0)), program.loadUpdates)
    }

    @Test
    fun `a failed weight save is reported`() = runTest {
        program.failWrites = true
        val viewModel = viewModel(dayId = 2)

        viewModel.updateLoadSettings(8, LoadSettings(startLoadKg = null, incrementKg = 5.0))

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
    }
}
