package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProgramCalendarTest {

    private val monday = LocalDate.of(2026, 9, 14)

    private val profile = Profile(
        programStart = monday,
        maintenanceKcal = 2450,
        surplusKcal = 350,
        kcalAdjustment = 0,
        proteinG = 145,
        fatG = 75,
    )

    private fun planned(sets: Int) = PlannedExercise(
        position = 1,
        exercise = Exercise(1, "Barbell bench press", LoadType.WEIGHT, Progression.DOUBLE, 40.0, 2.5, 20.0),
        sets = sets,
        reps = 5..8,
        rir = 2..2,
        restSeconds = 180..180,
        isTop = false,
        note = null,
        alternatives = emptyList(),
    )

    @Test
    fun `starting Monday to Thursday counts from that week's Monday`() {
        (0L..3L).forEach { assertEquals(monday, programStartFor(monday.plusDays(it))) }
    }

    @Test
    fun `starting Friday to Sunday counts from the next Monday`() {
        (4L..6L).forEach { assertEquals(monday.plusWeeks(1), programStartFor(monday.plusDays(it))) }
    }

    @Test
    fun `weeks run Monday to Sunday from the aligned start`() {
        val wednesday = monday.plusDays(2)

        assertEquals(1, programWeek(wednesday, monday))
        assertEquals(1, programWeek(wednesday, monday.plusDays(6)))
        assertEquals(2, programWeek(wednesday, monday.plusDays(7)))
        assertEquals(9, programWeek(wednesday, monday.plusWeeks(8)))
    }

    @Test
    fun `dates before the first program Monday have no week`() {
        assertNull(programWeek(monday, monday.minusDays(1)))
    }

    @Test
    fun `a weekend start still gets two full maintenance weeks`() {
        val sunday = monday.plusDays(6)

        assertNull(programWeek(sunday, sunday))
        val firstSurplusDay = (0L..30L).map { sunday.plusDays(it) }
            .first { profile.nutritionTargets(programWeek(sunday, it)).phase == NutritionPhase.SURPLUS }
        assertEquals(monday.plusWeeks(3), firstSurplusDay)
    }

    @Test
    fun `before the start and week one are calibration`() {
        assertTrue(isCalibrationWeek(null))
        assertTrue(isCalibrationWeek(1))
        assertFalse(isCalibrationWeek(2))
    }

    @Test
    fun `every eighth week is a deload`() {
        assertFalse(isDeloadWeek(null))
        assertFalse(isDeloadWeek(7))
        assertTrue(isDeloadWeek(8))
        assertFalse(isDeloadWeek(9))
        assertTrue(isDeloadWeek(16))
    }

    @Test
    fun `deload halves sets rounding up and leaves other weeks alone`() {
        val day = WorkoutDay(1, "Upper A", "press emphasis", DayOfWeek.MONDAY, listOf(planned(3), planned(2)))

        assertEquals(listOf(2, 1), day.forWeek(8).exercises.map { it.sets })
        assertEquals(day, day.forWeek(7))
        assertEquals(day, day.forWeek(null))
    }

    @Test
    fun `first two weeks hold maintenance`() {
        val targets = profile.nutritionTargets(week = 2)

        assertEquals(NutritionPhase.MAINTENANCE, targets.phase)
        assertEquals(2450, targets.kcal)
        assertEquals(298, targets.carbsG)
    }

    @Test
    fun `week three adds the surplus and carbs take the remainder`() {
        val targets = profile.nutritionTargets(week = 3)

        assertEquals(NutritionPhase.SURPLUS, targets.phase)
        assertEquals(2800, targets.kcal)
        assertEquals(145, targets.proteinG)
        assertEquals(75, targets.fatG)
        assertEquals(386, targets.carbsG)
    }

    @Test
    fun `calorie adjustment applies only once the surplus starts`() {
        val adjusted = profile.copy(kcalAdjustment = -150)

        assertEquals(2450, adjusted.nutritionTargets(week = 1).kcal)
        assertEquals(2650, adjusted.nutritionTargets(week = 3).kcal)
    }

    @Test
    fun `program not started targets maintenance`() {
        assertEquals(NutritionPhase.MAINTENANCE, profile.nutritionTargets(week = null).phase)
    }

    @Test
    fun `carbs never go negative`() {
        assertEquals(0, profile.copy(maintenanceKcal = 1000).nutritionTargets(week = 1).carbsG)
    }

    @Test
    fun `travel days have no workout`() {
        val days = DayOfWeek.entries.take(4).map { WorkoutDay(it.value.toLong(), it.name, "", it, emptyList()) }

        assertEquals(DayOfWeek.THURSDAY, days.forDate(monday.plusDays(3))?.dayOfWeek)
        assertNull(days.forDate(monday.plusDays(4)))
        assertNull(days.forDate(monday.plusDays(6)))
    }

    @Test
    fun `week on a date is empty until the program has a start`() {
        assertNull(profile.copy(programStart = null).weekOn(monday))
        assertEquals(2, profile.weekOn(monday.plusDays(7)))
    }

    @Test
    fun `next training day is today, a later day this week, or next Monday`() {
        val days = DayOfWeek.entries.take(4).reversed().map { WorkoutDay(it.value.toLong(), it.name, "", it, emptyList()) }

        assertEquals(DayOfWeek.WEDNESDAY, days.nextFrom(monday.plusDays(2))?.dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, days.nextFrom(monday.plusDays(4))?.dayOfWeek)
        assertNull(emptyList<WorkoutDay>().nextFrom(monday))
    }
}
