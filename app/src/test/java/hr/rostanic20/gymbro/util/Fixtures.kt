package hr.rostanic20.gymbro.util

import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.Progression
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import java.time.DayOfWeek

fun plannedExercise(
    id: Long,
    name: String,
    sets: Int = 3,
    loadType: LoadType = LoadType.WEIGHT,
    startLoadKg: Double? = null,
    barWeightKg: Double? = null,
): PlannedExercise = PlannedExercise(
    exercise = Exercise(
        id = id,
        name = name,
        loadType = loadType,
        progression = Progression.DOUBLE,
        startLoadKg = startLoadKg,
        incrementKg = if (loadType == LoadType.BODYWEIGHT) null else 2.5,
        barWeightKg = barWeightKg,
    ),
    sets = sets,
    reps = 5..8,
    rir = 2..2,
    restSeconds = 180..180,
    isTop = false,
    note = null,
    alternatives = emptyList(),
)

val weekProgram: List<WorkoutDay> = listOf(
    WorkoutDay(1, "Upper A", "press emphasis", DayOfWeek.MONDAY, listOf(plannedExercise(1, "Barbell bench press"))),
    WorkoutDay(2, "Lower A", "squat emphasis", DayOfWeek.TUESDAY, listOf(plannedExercise(8, "Back squat"))),
    WorkoutDay(3, "Upper B", "pull emphasis", DayOfWeek.WEDNESDAY, listOf(plannedExercise(13, "Pull-up"))),
    WorkoutDay(4, "Lower B", "hinge emphasis", DayOfWeek.THURSDAY, listOf(plannedExercise(20, "Romanian deadlift"))),
)
