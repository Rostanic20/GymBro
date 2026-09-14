package hr.rostanic20.gymbro.data

import hr.rostanic20.gymbro.data.local.UserProfile
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import java.time.DayOfWeek
import java.time.LocalDate

fun List<ProgramRowEntity>.toWorkoutDays(): List<WorkoutDay> =
    groupBy { it.day_id }.values.map { rows ->
        val day = rows.first()
        WorkoutDay(
            id = day.day_id,
            name = day.day_name,
            emphasis = day.emphasis,
            dayOfWeek = DayOfWeek.of(day.iso_day_of_week.toInt()),
            exercises = rows.map { it.toPlannedExercise() },
        )
    }

private fun ProgramRowEntity.toPlannedExercise(): PlannedExercise = PlannedExercise(
    exerciseId = exercise_id,
    name = exercise_name,
    sets = sets.toInt(),
    reps = rep_min.toInt()..rep_max.toInt(),
    rir = rir_min.toInt()..rir_max.toInt(),
    restSeconds = rest_seconds.toInt(),
    isTop = is_top != 0L,
    note = note,
    substitute = substitute,
    startLoadKg = start_load_kg,
    incrementKg = increment_kg,
    progression = Progression.valueOf(progression),
)

fun UserProfile.toDomain(): Profile = Profile(
    programStart = programStartEpochDay?.let(LocalDate::ofEpochDay),
    maintenanceKcal = maintenanceKcal,
    surplusKcal = surplusKcal,
    kcalAdjustment = kcalAdjustment,
    proteinG = proteinG,
    fatG = fatG,
)
