package hr.rostanic20.gymbro.ui.workout

import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.FakeProgramRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import hr.rostanic20.gymbro.util.weekProgram
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private val dates = FakeDateProvider(monday)

    private fun viewModel(dayId: Long) = WorkoutViewModel(dayId, profiles, program, dates)

    @Test
    fun `shows the requested day with deload sets in week eight`() = runTest {
        profiles.setProgramStart(monday)
        dates.date = monday.plusWeeks(7).plusDays(1)
        val viewModel = viewModel(dayId = 2)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val state = viewModel.state.value
        assertEquals("Lower A", state?.day?.name)
        assertEquals(8, state?.week)
        assertEquals(2, state?.day?.exercises?.single()?.sets)
    }

    @Test
    fun `an unknown day shows nothing`() = runTest {
        val viewModel = viewModel(dayId = 99)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        assertNull(viewModel.state.value?.day)
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
