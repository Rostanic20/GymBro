package hr.rostanic20.gymbro.ui.history

import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.util.FakeProgramRepository
import hr.rostanic20.gymbro.util.FakeSessionRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import hr.rostanic20.gymbro.util.weekProgram
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val sessions = FakeSessionRepository()
    private val viewModel by lazy { HistoryViewModel(sessions, FakeProgramRepository(weekProgram)) }

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `finished workouts are listed newest first with their day name and set count`() = runTest {
        val first = sessions.startSession(dayId = 1, date = monday, isDeload = false, nowMillis = 1_000)
        repeat(3) { sessions.logSet(first, 1, 1, SetValues(40.0, 8, 2), nowMillis = 1_100) }
        sessions.finishSession(first, nowMillis = 1_500)
        val second = sessions.startSession(dayId = 2, date = monday.plusDays(1), isDeload = true, nowMillis = 2_000)
        sessions.logSet(second, 8, 1, SetValues(60.0, 5, 2), nowMillis = 2_100)
        sessions.setNote(second, "Back felt tight")
        sessions.finishSession(second, nowMillis = 2_500)
        collectState()

        val items = viewModel.state.value!!
        assertEquals(listOf("Lower A", "Upper A"), items.map { it.dayName })
        assertEquals(listOf(1, 3), items.map { it.session.setCount })
        assertEquals("Back felt tight", items.first().session.note)
        assertTrue(items.first().session.isDeload)
    }

    @Test
    fun `an unfinished workout stays out of the history`() = runTest {
        sessions.startSession(dayId = 1, date = monday, isDeload = false, nowMillis = 1_000)
        collectState()

        assertTrue(viewModel.state.value!!.isEmpty())
    }

    @Test
    fun `a session from a day the program no longer has still shows up`() = runTest {
        val id = sessions.startSession(dayId = 99, date = monday, isDeload = false, nowMillis = 1_000)
        sessions.finishSession(id, nowMillis = 1_500)
        collectState()

        assertNull(viewModel.state.value!!.single().dayName)
    }
}
