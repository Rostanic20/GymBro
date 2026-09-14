package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.TopSetPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LiftTrendTest {

    private val day = LocalDate.of(2026, 9, 14)

    @Test
    fun `a weighted lift climbs with an extra rep at the same weight`() {
        val before = TopSetPoint(day, 60.0, 6).trendValue(LoadType.WEIGHT)
        val after = TopSetPoint(day, 60.0, 7).trendValue(LoadType.WEIGHT)

        assertEquals(72.0, before, 0.001)
        assertTrue(after > before)
    }

    @Test
    fun `an assisted lift climbs as the assistance drops`() {
        val before = TopSetPoint(day, 20.0, 8).trendValue(LoadType.ASSISTANCE)
        val after = TopSetPoint(day, 15.0, 6).trendValue(LoadType.ASSISTANCE)

        assertTrue(after > before)
    }

    @Test
    fun `a bodyweight lift is tracked by reps`() {
        assertEquals(12.0, TopSetPoint(day, null, 12).trendValue(LoadType.BODYWEIGHT), 0.001)
    }
}
