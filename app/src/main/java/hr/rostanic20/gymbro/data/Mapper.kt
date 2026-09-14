package hr.rostanic20.gymbro.data

import hr.rostanic20.gymbro.data.local.UserProfile
import hr.rostanic20.gymbro.domain.model.Alternative
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import java.time.DayOfWeek
import java.time.LocalDate

fun List<ProgramRowEntity>.toWorkoutDays(alternatives: List<AlternativeRowEntity>): List<WorkoutDay> {
    val alternativesByExercise = alternatives.groupBy({ it.exercise_id }, { it.toAlternative() })
    return groupBy { it.day_id }.values.map { rows ->
        val day = rows.first()
        WorkoutDay(
            id = day.day_id,
            name = day.day_name,
            emphasis = day.emphasis,
            dayOfWeek = DayOfWeek.of(day.iso_day_of_week.toInt()),
            exercises = rows.map { it.toPlannedExercise(alternativesByExercise[it.exercise_id].orEmpty()) },
        )
    }
}

private fun ProgramRowEntity.toPlannedExercise(alternatives: List<Alternative>): PlannedExercise = PlannedExercise(
    exercise = Exercise(
        id = exercise_id,
        name = exercise_name,
        loadType = LoadType.valueOf(load_type),
        progression = Progression.valueOf(progression),
        startLoadKg = start_load_kg,
        incrementKg = increment_kg,
        barWeightKg = bar_weight_kg,
    ),
    sets = sets.toInt(),
    reps = rep_min.toInt()..rep_max.toInt(),
    rir = rir_min.toInt()..rir_max.toInt(),
    restSeconds = rest_min_seconds.toInt()..rest_max_seconds.toInt(),
    isTop = is_top != 0L,
    note = note,
    alternatives = alternatives,
)

private fun AlternativeRowEntity.toAlternative(): Alternative = Alternative(
    exerciseId = alternative_id,
    name = alternative_name,
    note = note,
)

fun UserProfile.toDomain(): Profile = Profile(
    programStart = programStartEpochDay?.let(LocalDate::ofEpochDay),
    maintenanceKcal = maintenanceKcal,
    surplusKcal = surplusKcal,
    kcalAdjustment = kcalAdjustment,
    proteinG = proteinG,
    fatG = fatG,
)
