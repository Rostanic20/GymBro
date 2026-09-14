package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.BodyWeight
import java.time.LocalDate

private const val DAYS_BEFORE_IN_WEEK = 6L
private const val DAYS_PER_WEEK = 7L

fun List<BodyWeight>.averageForWeekEnding(date: LocalDate): Double? {
    val window = date.minusDays(DAYS_BEFORE_IN_WEEK)..date
    val weights = filter { it.date in window }.map { it.weightKg }
    return if (weights.isEmpty()) null else weights.average()
}

fun List<BodyWeight>.rollingWeeklyAverage(): List<BodyWeight> =
    sortedBy { it.date }.map { BodyWeight(it.date, averageForWeekEnding(it.date) ?: it.weightKg) }

fun List<BodyWeight>.weeklyChange(date: LocalDate): Double? {
    val current = averageForWeekEnding(date) ?: return null
    val previous = averageForWeekEnding(date.minusDays(DAYS_PER_WEEK)) ?: return null
    return current - previous
}
