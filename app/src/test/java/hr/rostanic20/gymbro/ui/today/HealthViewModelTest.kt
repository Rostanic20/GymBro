package hr.rostanic20.gymbro.ui.today

import hr.rostanic20.gymbro.core.HealthSummary
import hr.rostanic20.gymbro.util.FakeDateProvider
import hr.rostanic20.gymbro.util.FakeHealthSource
import hr.rostanic20.gymbro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 14)
    private val source = FakeHealthSource()
    private val dates = FakeDateProvider(today)
    private val viewModel by lazy { HealthViewModel(source, dates) }

    @Test
    fun `a phone without Health Connect says so`() = runTest {
        source.available = false

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(HealthUiState.Unavailable, viewModel.state.value)
    }

    @Test
    fun `without permission the card asks to connect`() = runTest {
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(HealthUiState.NeedsPermission, viewModel.state.value)
    }

    @Test
    fun `once connected today's summary is shown and re-read on refresh`() = runTest {
        source.granted = true
        source.summary = HealthSummary(steps = 8_412, sleep = Duration.ofMinutes(435))

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(HealthUiState.Connected(8_412, Duration.ofMinutes(435)), viewModel.state.value)

        dates.date = today.plusDays(1)
        source.summary = HealthSummary(steps = 120, sleep = null)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(HealthUiState.Connected(120, null), viewModel.state.value)
        assertEquals(listOf(today, today.plusDays(1)), source.requestedDates)
    }

    @Test
    fun `a read failure offers a retry instead of crashing`() = runTest {
        source.granted = true
        source.fail = true

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(HealthUiState.Failed, viewModel.state.value)
    }
}
