package hr.rostanic20.gymbro.ui.today

import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeBodyRepository
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakeFoodRepository
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
    private val sessions = FakeSessionRepository()
    private val foods = FakeFoodRepository()
    private val body = FakeBodyRepository()
    private val dates = FakeDateProvider(monday)
    private val viewModel by lazy {
        TodayViewModel(profiles, FakeProgramRepository(weekProgram), sessions, foods, body, dates)
    }

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

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
        collectState()

        assertEquals(2, viewModel.state.value?.week)
        assertEquals(NutritionPhase.MAINTENANCE, viewModel.state.value?.targets?.phase)

        dates.date = monday.plusDays(14)

        assertEquals(3, viewModel.state.value?.week)
        assertEquals(NutritionPhase.SURPLUS, viewModel.state.value?.targets?.phase)
    }

    @Test
    fun `today's workout moves from not started to in progress to logged`() = runTest {
        collectState()
        assertEquals(WorkoutStatus.NOT_STARTED, viewModel.state.value?.workoutStatus)

        val sessionId = sessions.startSession(dayId = 1, date = monday, isDeload = false, nowMillis = 1_000)
        assertEquals(WorkoutStatus.IN_PROGRESS, viewModel.state.value?.workoutStatus)

        sessions.finishSession(sessionId, nowMillis = 2_000)
        assertEquals(WorkoutStatus.LOGGED, viewModel.state.value?.workoutStatus)
    }

    @Test
    fun `another day's unfinished session does not mark today in progress`() = runTest {
        sessions.startSession(dayId = 4, date = monday.minusDays(4), isDeload = false, nowMillis = 1_000)
        collectState()

        assertEquals(WorkoutStatus.NOT_STARTED, viewModel.state.value?.workoutStatus)
    }

    @Test
    fun `eaten totals add up today's log, per meal`() = runTest {
        collectState()

        foods.logEstimate(monday, Meal.LUNCH, "Canteen", Nutrition(650.0, 28.0, 70.0, 22.0), nowMillis = 1)
        foods.logEstimate(monday, Meal.DINNER, "Eggs and toast", Nutrition(415.0, 25.0, 30.0, 20.0), nowMillis = 2)
        foods.logEstimate(monday.minusDays(1), Meal.DINNER, "Yesterday", Nutrition(999.0, 9.0, 9.0, 9.0), nowMillis = 3)

        val state = viewModel.state.value!!
        assertEquals(1065.0, state.eaten.kcal, 0.001)
        assertEquals(53.0, state.eaten.proteinG, 0.001)
        assertEquals(650.0, state.eatenByMeal.getValue(Meal.LUNCH).kcal, 0.001)
        assertNull(state.eatenByMeal[Meal.BREAKFAST_SHAKE])
    }

    @Test
    fun `a weigh-in shows today's weight, the weekly average and the change`() = runTest {
        (7L..13L).forEach { body.setWeight(monday.minusDays(it), 70.0) }
        (1L..6L).forEach { body.setWeight(monday.minusDays(it), 70.3) }
        collectState()

        viewModel.saveWeight(70.3)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(70.3, state.weightTodayKg!!, 0.001)
        assertEquals(70.3, state.weekAverageKg!!, 0.001)
        assertEquals(0.3, state.weeklyChangeKg!!, 0.001)
    }
}
