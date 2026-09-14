package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Exercise
import kotlin.math.roundToInt

data class WarmupSet(val loadKg: Double, val reps: Int)

private const val EMPTY_BAR_REPS = 10
private const val PLATE_STEP_KG = 2.5
private val RAMP = listOf(0.5 to 5, 0.7 to 3, 0.85 to 1)

fun Exercise.warmupRamp(workingLoadKg: Double): List<WarmupSet> {
    val bar = barWeightKg ?: return emptyList()
    if (workingLoadKg <= bar) return emptyList()
    val ramp = RAMP.map { (fraction, reps) ->
        WarmupSet(roundToPlates(workingLoadKg * fraction).coerceAtLeast(bar), reps)
    }
    return (listOf(WarmupSet(bar, EMPTY_BAR_REPS)) + ramp)
        .filter { it.loadKg < workingLoadKg }
        .distinctBy { it.loadKg }
}

private fun roundToPlates(kg: Double): Double = (kg / PLATE_STEP_KG).roundToInt() * PLATE_STEP_KG
