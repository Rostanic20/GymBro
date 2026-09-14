package hr.rostanic20.gymbro.ui.train

import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.workout.LoadSettings
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.FakeProgramRepository
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
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TrainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val profiles = FakeProfileRepository()
    private val program = FakeProgramRepository(weekProgram)
    private val dates = FakeDateProvider(monday)
    private val viewModel by lazy { TrainViewModel(profiles, program, dates) }

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `today's day is the one expanded on a training day`() = runTest {
        dates.date = monday.plusDays(2)
        collectState()

        assertEquals(3L, viewModel.state.value?.defaultExpandedDayId)
    }

    @Test
    fun `on a weekend the coming Monday is expanded`() = runTest {
        dates.date = monday.plusDays(5)
        collectState()

        assertEquals(1L, viewModel.state.value?.defaultExpandedDayId)
    }

    @Test
    fun `a deload week halves every day's sets`() = runTest {
        profiles.setProgramStart(monday)
        dates.date = monday.plusWeeks(7)
        collectState()

        assertEquals(8, viewModel.state.value?.week)
        assertEquals(listOf(2, 2, 2, 2), viewModel.state.value?.days?.map { it.exercises.single().sets })
    }

    @Test
    fun `saving weights passes the values through`() = runTest {
        viewModel.updateLoadSettings(8, LoadSettings(startLoadKg = 45.0, incrementKg = 5.0))
        advanceUntilIdle()

        assertEquals(listOf(Triple(8L, 45.0, 5.0)), program.loadUpdates)
    }

    @Test
    fun `a failed weight save is reported`() = runTest {
        program.failWrites = true

        viewModel.updateLoadSettings(8, LoadSettings(startLoadKg = null, incrementKg = 5.0))

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
    }
}
