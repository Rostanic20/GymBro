package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.domain.model.NutritionTargets
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

const val MAINTENANCE_WEEKS = 2
const val DELOAD_EVERY_WEEKS = 8
const val KCAL_PER_G_PROTEIN = 4
const val KCAL_PER_G_CARBS = 4
const val KCAL_PER_G_FAT = 9

private const val CALIBRATION_WEEKS = 1
private const val DAYS_PER_WEEK = 7L

fun programStartFor(date: LocalDate): LocalDate = when (date.dayOfWeek) {
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY ->
        date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    else -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

fun programWeek(start: LocalDate, date: LocalDate): Int? {
    val firstMonday = programStartFor(start)
    if (date < firstMonday) return null
    return (ChronoUnit.DAYS.between(firstMonday, date) / DAYS_PER_WEEK).toInt() + 1
}

fun Profile.weekOn(date: LocalDate): Int? = programStart?.let { programWeek(it, date) }

fun isCalibrationWeek(week: Int?): Boolean = week == null || week <= CALIBRATION_WEEKS

fun isDeloadWeek(week: Int?): Boolean = week != null && week % DELOAD_EVERY_WEEKS == 0

fun WorkoutDay.forWeek(week: Int?): WorkoutDay =
    if (!isDeloadWeek(week)) this else copy(exercises = exercises.map { it.copy(sets = (it.sets + 1) / 2) })

fun Profile.nutritionTargets(week: Int?): NutritionTargets {
    val phase = if (week != null && week > MAINTENANCE_WEEKS) NutritionPhase.SURPLUS else NutritionPhase.MAINTENANCE
    val kcal = when (phase) {
        NutritionPhase.MAINTENANCE -> maintenanceKcal
        NutritionPhase.SURPLUS -> maintenanceKcal + surplusKcal + kcalAdjustment
    }
    val carbsKcal = kcal - proteinG * KCAL_PER_G_PROTEIN - fatG * KCAL_PER_G_FAT
    return NutritionTargets(
        phase = phase,
        kcal = kcal,
        proteinG = proteinG,
        fatG = fatG,
        carbsG = (carbsKcal / KCAL_PER_G_CARBS).coerceAtLeast(0),
    )
}

fun List<WorkoutDay>.forDate(date: LocalDate): WorkoutDay? =
    firstOrNull { it.dayOfWeek == date.dayOfWeek }

fun List<WorkoutDay>.nextFrom(date: LocalDate): WorkoutDay? {
    val byWeekday = sortedBy { it.dayOfWeek }
    return byWeekday.firstOrNull { it.dayOfWeek >= date.dayOfWeek } ?: byWeekday.firstOrNull()
}
