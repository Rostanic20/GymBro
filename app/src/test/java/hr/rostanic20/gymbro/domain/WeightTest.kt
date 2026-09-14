package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.BodyWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightTest {

    private val monday = LocalDate.of(2026, 9, 14)

    private fun weights(start: LocalDate, vararg kg: Double) =
        kg.mapIndexed { index, value -> BodyWeight(start.plusDays(index.toLong()), value) }

    @Test
    fun `the weekly average uses the last seven days, whatever was logged`() {
        val entries = weights(monday, 70.0, 70.4, 70.2) + BodyWeight(monday.minusDays(7), 60.0)

        assertEquals(70.2, entries.averageForWeekEnding(monday.plusDays(6))!!, 0.001)
    }

    @Test
    fun `no weigh-ins in the window means no average`() {
        assertNull(weights(monday, 70.0).averageForWeekEnding(monday.minusDays(1)))
    }

    @Test
    fun `weekly change compares this week's average with the week before`() {
        val entries = weights(monday.minusDays(7), 70.0, 70.2, 70.1, 69.9, 70.0, 70.3, 70.2) +
            weights(monday, 70.4, 70.3, 70.6, 70.5, 70.2, 70.6, 70.5)

        assertEquals(0.343, entries.weeklyChange(monday.plusDays(6))!!, 0.001)
    }

    @Test
    fun `the rolling average smooths each weigh-in over its trailing week`() {
        val entries = weights(monday, 70.0, 71.0, 69.0) + BodyWeight(monday.plusDays(10), 72.0)

        assertEquals(
            listOf(70.0, 70.5, 70.0, 72.0),
            entries.shuffled().rollingWeeklyAverage().map { it.weightKg },
        )
    }

    @Test
    fun `weekly change needs weigh-ins in both weeks`() {
        assertNull(weights(monday, 70.4, 70.3).weeklyChange(monday.plusDays(6)))
    }
}
