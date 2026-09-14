package hr.rostanic20.gymbro.ui.settings

import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val profiles = FakeProfileRepository()
    private val viewModel by lazy { SettingsViewModel(profiles) }

    @Test
    fun `a Saturday pick starts the program the following Monday`() = runTest {
        viewModel.changeProgramStart(monday.plusDays(5))
        advanceUntilIdle()

        assertEquals(monday.plusWeeks(1), profiles.current.programStart)
    }

    @Test
    fun `reset clears the start date`() = runTest {
        profiles.setProgramStart(monday)

        viewModel.resetProgram()
        advanceUntilIdle()

        assertNull(profiles.current.programStart)
    }

    @Test
    fun `saved targets are stored and confirmed`() = runTest {
        viewModel.saveTargets(Targets(maintenanceKcal = 2600, surplusKcal = 300, proteinG = 150, fatG = 80))

        assertEquals(UserMessage.TargetsSaved, viewModel.messages.first())
        assertEquals(2600, profiles.current.maintenanceKcal)
        assertEquals(300, profiles.current.surplusKcal)
    }

    @Test
    fun `a failed targets save is reported and not confirmed`() = runTest {
        profiles.failWrites = true

        viewModel.saveTargets(Targets(maintenanceKcal = 2600, surplusKcal = 300, proteinG = 150, fatG = 80))

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
        assertEquals(2450, profiles.current.maintenanceKcal)
    }
}
