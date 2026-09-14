package hr.rostanic20.gymbro.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.PlannedExercise

private const val SECONDS_PER_MINUTE = 60

@Composable
fun rangeLabel(range: IntRange): String =
    if (range.first == range.last) {
        range.first.toString()
    } else {
        stringResource(R.string.range, range.first, range.last)
    }

@Composable
fun setsRepsLabel(exercise: PlannedExercise): String =
    stringResource(R.string.sets_reps, exercise.sets, rangeLabel(exercise.reps))

@Composable
fun restLabel(seconds: Int): String {
    if (seconds < 2 * SECONDS_PER_MINUTE) return stringResource(R.string.rest_seconds, seconds)
    val minutes = if (seconds % SECONDS_PER_MINUTE == 0) {
        (seconds / SECONDS_PER_MINUTE).toString()
    } else {
        "%.1f".format(seconds.toDouble() / SECONDS_PER_MINUTE)
    }
    return stringResource(R.string.rest_minutes, minutes)
}
