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
    val exerciseId: Long,
    val name: String,
    val sets: Int,
    val reps: IntRange,
    val rir: IntRange,
    val restSeconds: Int,
    val isTop: Boolean,
    val note: String?,
    val substitute: String?,
    val startLoadKg: Double?,
    val incrementKg: Double,
    val progression: Progression,
)

enum class Progression { LOAD, REPS_FIRST, BODYWEIGHT }
