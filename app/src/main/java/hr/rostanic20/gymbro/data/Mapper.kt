package hr.rostanic20.gymbro.data

import hr.rostanic20.gymbro.data.local.UserProfile
import hr.rostanic20.gymbro.domain.model.Alternative
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.model.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate

fun List<ProgramRowEntity>.toWorkoutDays(alternatives: List<AlternativeRowEntity>): List<WorkoutDay> {
    val alternativesByExercise = alternatives.groupBy({ it.slot_exercise_id }, { it.toAlternative() })
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
    position = position.toInt(),
    exercise = exercise(
        id = exercise_id,
        name = exercise_name,
        loadType = load_type,
        progression = progression,
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
    exercise = exercise(
        id = id,
        name = name,
        loadType = load_type,
        progression = progression,
        startLoadKg = start_load_kg,
        incrementKg = increment_kg,
        barWeightKg = bar_weight_kg,
    ),
    note = alternative_note,
)

private fun exercise(
    id: Long,
    name: String,
    loadType: String,
    progression: String,
    startLoadKg: Double?,
    incrementKg: Double?,
    barWeightKg: Double?,
): Exercise = Exercise(
    id = id,
    name = name,
    loadType = LoadType.valueOf(loadType),
    progression = Progression.valueOf(progression),
    startLoadKg = startLoadKg,
    incrementKg = incrementKg,
    barWeightKg = barWeightKg,
)

fun SessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    dayId = day_id,
    date = LocalDate.ofEpochDay(date_epoch_day),
    startedAtMillis = started_at,
    finishedAtMillis = finished_at,
    isDeload = is_deload != 0L,
    note = note,
    restEndsAtMillis = rest_ends_at,
)

fun SetEntity.toDomain(): LoggedSet = LoggedSet(
    id = id,
    exerciseId = exercise_id,
    slotPosition = slot_position.toInt(),
    loadKg = load_kg,
    reps = reps.toInt(),
    rir = rir?.toInt(),
)

fun TopSetRowEntity.toDomain(): TopSet = TopSet(loadKg = load_kg, reps = reps.toInt())

fun UserProfile.toDomain(): Profile = Profile(
    programStart = programStartEpochDay?.let(LocalDate::ofEpochDay),
    maintenanceKcal = maintenanceKcal,
    surplusKcal = surplusKcal,
    kcalAdjustment = kcalAdjustment,
    proteinG = proteinG,
    fatG = fatG,
)
