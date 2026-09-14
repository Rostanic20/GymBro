package hr.rostanic20.gymbro.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.PlannedExercise

private const val SECONDS_PER_MINUTE = 60
private const val SHOW_AS_MINUTES_FROM_SECONDS = 120

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
fun restLabel(seconds: IntRange): String {
    val wholeMinutes = seconds.first % SECONDS_PER_MINUTE == 0 && seconds.last % SECONDS_PER_MINUTE == 0
    return if (wholeMinutes && seconds.first >= SHOW_AS_MINUTES_FROM_SECONDS) {
        stringResource(
            R.string.rest_minutes,
            rangeLabel(seconds.first / SECONDS_PER_MINUTE..seconds.last / SECONDS_PER_MINUTE),
        )
    } else {
        stringResource(R.string.rest_seconds, rangeLabel(seconds))
    }
}
