package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.WaistMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyCheckTest {

    private val today = LocalDate.of(2026, 10, 19)

    private fun threeWeeks(first: Double, second: Double, third: Double): List<BodyWeight> =
        (0L..20L).map { daysAgo ->
            val kg = when {
                daysAgo >= 14 -> first
                daysAgo >= 7 -> second
                else -> third
            }
            BodyWeight(today.minusDays(daysAgo), kg)
        }

    private fun check(
        weights: List<BodyWeight> = threeWeeks(70.0, 70.3, 70.6),
        waists: List<WaistMeasurement> = emptyList(),
        week: Int? = 6,
        lastAdjustment: LocalDate? = null,
    ) = weeklyCheck(week, today, weights, waists, lastAdjustment)

    @Test
    fun `nothing is judged before week five, while water weight settles`() {
        assertEquals(WeeklyCheck.TooEarly, check(week = null))
        assertEquals(WeeklyCheck.TooEarly, check(week = 4))
    }

    @Test
    fun `a recent adjustment waits two weeks before re-checking`() {
        assertEquals(
            WeeklyCheck.RecentlyAdjusted(today.plusDays(4)),
            check(weights = threeWeeks(70.0, 70.0, 70.0), lastAdjustment = today.minusDays(10)),
        )
        assertEquals(WeeklyCheck.EatMore(0.0), check(weights = threeWeeks(70.0, 70.0, 70.0), lastAdjustment = today.minusDays(14)))
    }

    @Test
    fun `two weeks of weigh-ins are needed to compare`() {
        assertEquals(WeeklyCheck.NotEnoughData, check(weights = threeWeeks(70.0, 70.3, 70.6).filter { it.date > today.minusDays(14) }))
    }

    @Test
    fun `0_3 kg a week is on track`() {
        val result = check()

        assertTrue(result is WeeklyCheck.OnTrack)
        assertEquals(0, result.calorieChange)
    }

    @Test
    fun `two flat weeks means eat more`() {
        val result = check(weights = threeWeeks(70.0, 70.05, 70.1))

        assertTrue(result is WeeklyCheck.EatMore)
        assertEquals(CALORIE_STEP, result.calorieChange)
    }

    @Test
    fun `one flat week alone is not enough`() {
        assertTrue(check(weights = threeWeeks(70.0, 70.4, 70.45)) is WeeklyCheck.OnTrack)
    }

    @Test
    fun `gaining faster than half a kilo two weeks running means eat less`() {
        val result = check(weights = threeWeeks(70.0, 70.6, 71.2))

        assertTrue(result is WeeklyCheck.EatLessFastGain)
        assertEquals(-CALORIE_STEP, result.calorieChange)
    }

    @Test
    fun `a waist up a centimetre in a month outranks the scale`() {
        val waists = listOf(WaistMeasurement(today.minusDays(28), 76.0), WaistMeasurement(today, 77.2))

        val result = check(waists = waists)

        assertEquals(1.2, (result as WeeklyCheck.EatLessWaist).waistGainCm, 0.001)
        assertEquals(-CALORIE_STEP, result.calorieChange)
    }

    @Test
    fun `waist gain needs a measurement at least four weeks older`() {
        assertNull(waistGain(listOf(WaistMeasurement(today.minusDays(21), 76.0), WaistMeasurement(today, 78.0)), today))
        assertEquals(0.5, waistGain(listOf(WaistMeasurement(today.minusDays(35), 76.0), WaistMeasurement(today, 76.5)), today)!!, 0.001)
    }
}
