package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.TopSetPoint

private const val EPLEY_REPS_DIVISOR = 30.0

fun TopSetPoint.trendValue(loadType: LoadType): Double = when (loadType) {
    LoadType.WEIGHT -> loadKg?.let { it * (1 + reps / EPLEY_REPS_DIVISOR) } ?: reps.toDouble()
    LoadType.ASSISTANCE -> reps - (loadKg ?: 0.0)
    LoadType.BODYWEIGHT -> reps.toDouble()
}
