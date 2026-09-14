package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.TopSet

const val STALL_SESSIONS = 3

sealed interface Suggestion {
    data class FirstTime(val loadKg: Double?) : Suggestion
    data class Calibrate(val loadKg: Double?) : Suggestion
    data class Repeat(val loadKg: Double?, val lastReps: List<Int>) : Suggestion
    data class Increase(val loadKg: Double) : Suggestion
    data class Deload(val loadKg: Double?) : Suggestion
    data object AddBelt : Suggestion
    data object MakeItHarder : Suggestion
}

fun suggestNext(
    planned: PlannedExercise,
    exercise: Exercise,
    lastSets: List<LoggedSet>,
    week: Int?,
    isDeload: Boolean,
): Suggestion {
    val lastLoad = workingLoad(exercise.loadType, lastSets)
    if (isDeload) return Suggestion.Deload(lastLoad ?: exercise.startLoadKg)
    if (isCalibrationWeek(week)) return Suggestion.Calibrate(lastLoad ?: exercise.startLoadKg)
    if (lastSets.isEmpty()) return Suggestion.FirstTime(exercise.startLoadKg)

    val workingSets = lastSets.filter { it.loadKg == lastLoad }
    val repeat = Suggestion.Repeat(lastLoad, workingSets.map { it.reps })
    val hitTop = workingSets.size >= planned.sets &&
        workingSets.all { it.reps >= planned.reps.last && (it.rir == null || it.rir >= planned.rir.first) }
    if (!hitTop) return repeat

    val increment = exercise.incrementKg
    return when (exercise.loadType) {
        LoadType.WEIGHT ->
            if (lastLoad == null || increment == null) repeat else Suggestion.Increase(lastLoad + increment)
        LoadType.ASSISTANCE -> when {
            lastLoad == null || increment == null -> repeat
            lastLoad <= 0.0 -> Suggestion.AddBelt
            else -> Suggestion.Increase((lastLoad - increment).coerceAtLeast(0.0))
        }
        LoadType.BODYWEIGHT -> Suggestion.MakeItHarder
    }
}

fun workingLoad(loadType: LoadType, sets: List<LoggedSet>): Double? {
    val loads = sets.mapNotNull { it.loadKg }
    return when (loadType) {
        LoadType.WEIGHT -> loads.maxOrNull()
        LoadType.ASSISTANCE -> loads.minOrNull()
        LoadType.BODYWEIGHT -> null
    }
}

fun needsDeload(loadType: LoadType, recentTopSets: List<TopSet>): Boolean {
    if (recentTopSets.size < STALL_SESSIONS) return false
    val (latest, previous, before) = recentTopSets
    return lostReps(loadType, latest, previous) && lostReps(loadType, previous, before)
}

private fun lostReps(loadType: LoadType, newer: TopSet, older: TopSet): Boolean {
    val newerLoad = newer.loadKg ?: 0.0
    val olderLoad = older.loadKg ?: 0.0
    val sameOrEasier = when (loadType) {
        LoadType.WEIGHT -> newerLoad <= olderLoad
        LoadType.ASSISTANCE -> newerLoad >= olderLoad
        LoadType.BODYWEIGHT -> true
    }
    return sameOrEasier && newer.reps < older.reps
}
