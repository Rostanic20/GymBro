package hr.rostanic20.gymbro.ui.settings

import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeMealReminderScheduler
import hr.rostanic20.gymbro.util.FakeMealSettingsRepository
import hr.rostanic20.gymbro.util.FakeProfileRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 9, 14)
    private val profiles = FakeProfileRepository()
    private val mealSettings = FakeMealSettingsRepository()
    private val scheduler = FakeMealReminderScheduler()
    private val viewModel by lazy { SettingsViewModel(profiles, mealSettings, scheduler) }

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

    @Test
    fun `turning reminders on saves it and schedules the next one`() = runTest {
        viewModel.setMealReminders(true)
        advanceUntilIdle()

        assertTrue(profiles.current.mealRemindersEnabled)
        assertEquals(listOf(true), scheduler.calls)
    }

    @Test
    fun `changing a meal time keeps the reminders in step`() = runTest {
        profiles.setMealReminders(true)

        viewModel.setMealTime(Meal.DESK_SNACK, LocalTime.of(10, 30))
        advanceUntilIdle()

        assertEquals(LocalTime.of(10, 30), mealSettings.current.timeFor(Meal.DESK_SNACK))
        assertEquals(listOf(true), scheduler.calls)
    }

    @Test
    fun `turning a meal off drops it from the reminder list`() = runTest {
        viewModel.setMealEnabled(Meal.DESK_SNACK, enabled = false)
        advanceUntilIdle()

        assertTrue(Meal.DESK_SNACK !in mealSettings.current.activeMeals)
        assertEquals(listOf(false), scheduler.calls)
    }

    @Test
    fun `reminders are not rescheduled when saving the choice fails`() = runTest {
        profiles.failWrites = true

        viewModel.setMealReminders(true)

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
        assertTrue(scheduler.calls.isEmpty())
    }
}
