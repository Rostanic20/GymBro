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

private const val KCAL_PER_G_PROTEIN = 4
private const val KCAL_PER_G_CARBS = 4
private const val KCAL_PER_G_FAT = 9

fun programWeek(start: LocalDate, date: LocalDate): Int? {
    val startMonday = start.mondayOfWeek()
    val dateMonday = date.mondayOfWeek()
    if (dateMonday < startMonday) return null
    return ChronoUnit.WEEKS.between(startMonday, dateMonday).toInt() + 1
}

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

private fun LocalDate.mondayOfWeek(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
