package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `week one is the whole calendar week of the start date`() {
        val wednesday = monday.plusDays(2)

        assertEquals(1, programWeek(wednesday, monday))
        assertEquals(1, programWeek(wednesday, monday.plusDays(6)))
        assertEquals(2, programWeek(wednesday, monday.plusDays(7)))
        assertEquals(9, programWeek(wednesday, monday.plusWeeks(8)))
    }

    @Test
    fun `dates before the start week have no program week`() {
        assertNull(programWeek(monday, monday.minusDays(1)))
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
}
