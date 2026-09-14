package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MealReminderTest {

    private val monday = LocalDate.of(2026, 9, 14)

    @Test
    fun `early on a weekday the breakfast shake is next`() {
        assertEquals(
            MealReminder(Meal.BREAKFAST_SHAKE, monday.atTime(7, 0)),
            nextMealReminder(monday.atTime(6, 30)),
        )
    }

    @Test
    fun `a reminder at exactly now is already past`() {
        assertEquals(
            MealReminder(Meal.DESK_SNACK, monday.atTime(10, 0)),
            nextMealReminder(monday.atTime(7, 0)),
        )
    }

    @Test
    fun `after dinner the next reminder is tomorrow's shake`() {
        assertEquals(
            MealReminder(Meal.BREAKFAST_SHAKE, monday.plusDays(1).atTime(7, 0)),
            nextMealReminder(monday.atTime(21, 0)),
        )
    }

    @Test
    fun `at home from Friday only the two shakes are reminded`() {
        val friday = monday.plusDays(4)

        assertEquals(Meal.POST_GYM_SHAKE, nextMealReminder(friday.atTime(8, 0))?.meal)
        assertEquals(LocalTime.of(18, 45), nextMealReminder(friday.atTime(8, 0))?.at?.toLocalTime())
    }

    @Test
    fun `Sunday evening rolls over to Monday's full schedule`() {
        val sunday = monday.plusDays(6)

        assertEquals(
            MealReminder(Meal.BREAKFAST_SHAKE, monday.plusWeeks(1).atTime(7, 0)),
            nextMealReminder(sunday.atTime(19, 0)),
        )
        assertEquals(Meal.DESK_SNACK, nextMealReminder(monday.plusWeeks(1).atTime(7, 30))?.meal)
    }
}
