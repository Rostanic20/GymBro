package hr.rostanic20.gymbro.ui.today

import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.FakeProgramRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
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
class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val profiles = FakeProfileRepository()
    private val dates = FakeDateProvider(monday)
    private val viewModel by lazy { TodayViewModel(profiles, FakeProgramRepository(), dates) }

    @Test
    fun `starting on a Sunday stores the next Monday`() = runTest {
        dates.date = monday.plusDays(6)

        viewModel.startProgram()
        advanceUntilIdle()

        assertEquals(monday.plusWeeks(1), profiles.current.programStart)
    }

    @Test
    fun `starting mid-week stores that week's Monday`() = runTest {
        dates.date = monday.plusDays(2)

        viewModel.startProgram()
        advanceUntilIdle()

        assertEquals(monday, profiles.current.programStart)
    }

    @Test
    fun `a failed save is reported instead of swallowed`() = runTest {
        profiles.failWrites = true

        viewModel.startProgram()

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
        assertNull(profiles.current.programStart)
    }

    @Test
    fun `state follows the date across midnight`() = runTest {
        profiles.setProgramStart(monday)
        dates.date = monday.plusDays(13)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        assertEquals(2, viewModel.state.value?.week)
        assertEquals(NutritionPhase.MAINTENANCE, viewModel.state.value?.targets?.phase)

        dates.date = monday.plusDays(14)

        assertEquals(3, viewModel.state.value?.week)
        assertEquals(NutritionPhase.SURPLUS, viewModel.state.value?.targets?.phase)
    }
}
