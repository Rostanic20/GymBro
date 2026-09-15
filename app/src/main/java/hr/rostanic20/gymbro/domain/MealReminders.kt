package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class MealReminder(val meal: Meal, val at: LocalDateTime)

private val HOME_DAYS = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
private const val SEARCH_DAYS = 8L

fun Meal.isRemindedOn(date: LocalDate): Boolean = onWeekends || date.dayOfWeek !in HOME_DAYS

fun nextMealReminder(now: LocalDateTime, settings: MealSettings = MealSettings()): MealReminder? =
    (0L until SEARCH_DAYS).asSequence()
        .map { now.toLocalDate().plusDays(it) }
        .flatMap { date ->
            settings.activeMeals
                .filter { it.isRemindedOn(date) }
                .map { MealReminder(it, date.atTime(settings.timeFor(it))) }
        }
        .firstOrNull { it.at > now }
