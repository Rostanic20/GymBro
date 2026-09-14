package hr.rostanic20.gymbro.domain.model

import java.time.DayOfWeek

data class WorkoutDay(
    val id: Long,
    val name: String,
    val emphasis: String,
    val dayOfWeek: DayOfWeek,
    val exercises: List<PlannedExercise>,
) {
    val workingSets: Int get() = exercises.sumOf { it.sets }
}

data class PlannedExercise(
    val position: Int,
    val exercise: Exercise,
    val sets: Int,
    val reps: IntRange,
    val rir: IntRange,
    val restSeconds: IntRange,
    val isTop: Boolean,
    val note: String?,
    val alternatives: List<Alternative>,
)

data class Exercise(
    val id: Long,
    val name: String,
    val loadType: LoadType,
    val progression: Progression,
    val startLoadKg: Double?,
    val incrementKg: Double?,
    val barWeightKg: Double?,
)

data class Alternative(
    val exercise: Exercise,
    val note: String?,
)

enum class LoadType { WEIGHT, ASSISTANCE, BODYWEIGHT }

enum class Progression { DOUBLE, REPS_FIRST }
