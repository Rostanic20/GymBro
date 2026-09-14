package hr.rostanic20.gymbro.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SystemDateProviderTest {

    private val zagreb = ZoneId.of("Europe/Zagreb")

    @Test
    fun `emits the next date right after midnight`() = runTest {
        val start = ZonedDateTime.of(2026, 9, 14, 23, 59, 0, 0, zagreb)
        val provider = SystemDateProvider { start.plus(Duration.ofMillis(testScheduler.currentTime)) }

        val dates = provider.todayFlow().take(2).toList()

        assertEquals(listOf(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15)), dates)
        assertEquals(60_001L, testScheduler.currentTime)
    }

    @Test
    fun `waking a moment early does not skip the date change`() = runTest {
        val start = ZonedDateTime.of(2026, 9, 14, 23, 59, 59, 999_000_000, zagreb)
        val provider = SystemDateProvider { start.plus(Duration.ofMillis(testScheduler.currentTime)) }

        val dates = provider.todayFlow().take(2).toList()

        assertEquals(listOf(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15)), dates)
        assertEquals(1_000L, testScheduler.currentTime)
    }

    @Test
    fun `daylight saving night still changes date once`() = runTest {
        val start = ZonedDateTime.of(2026, 10, 24, 23, 0, 0, 0, zagreb)
        val provider = SystemDateProvider { start.plus(Duration.ofMillis(testScheduler.currentTime)) }

        val dates = provider.todayFlow().take(3).toList()

        assertEquals(
            listOf(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26)),
            dates,
        )
    }
}
