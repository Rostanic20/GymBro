package hr.rostanic20.gymbro.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HealthWindowsTest {

    private val zagreb = ZoneId.of("Europe/Zagreb")
    private val today = LocalDate.of(2026, 9, 14)

    @Test
    fun `steps for today run from midnight to now`() {
        val now = today.atTime(15, 30).atZone(zagreb)

        assertEquals(TimeWindow(today.atStartOfDay(zagreb), now), stepsWindow(today, now))
    }

    @Test
    fun `steps for a past day cover the whole day`() {
        val now = today.atTime(9, 0).atZone(zagreb)
        val yesterday = today.minusDays(1)

        assertEquals(TimeWindow(yesterday.atStartOfDay(zagreb), today.atStartOfDay(zagreb)), stepsWindow(yesterday, now))
    }

    @Test
    fun `last night's sleep is looked for from six in the evening to noon`() {
        assertEquals(
            TimeWindow(today.minusDays(1).atTime(18, 0).atZone(zagreb), today.atTime(12, 0).atZone(zagreb)),
            sleepWindow(today, zagreb),
        )
    }
}
