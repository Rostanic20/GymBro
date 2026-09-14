package hr.rostanic20.gymbro.ui.session

import hr.rostanic20.gymbro.domain.Suggestion
import hr.rostanic20.gymbro.domain.workingLoad

data class SetPrefill(val loadKg: Double?, val reps: Int?)

fun SlotState.prefill(): SetPrefill {
    sets.lastOrNull()?.let { return SetPrefill(it.loadKg, it.reps) }
    val bottom = planned.reps.first
    return when (val next = suggestion) {
        is Suggestion.FirstTime -> SetPrefill(next.loadKg, bottom)
        is Suggestion.Calibrate -> SetPrefill(next.loadKg, bottom)
        is Suggestion.Repeat -> SetPrefill(next.loadKg, next.lastReps.firstOrNull() ?: bottom)
        is Suggestion.Increase -> SetPrefill(next.loadKg, bottom)
        is Suggestion.Deload -> SetPrefill(next.loadKg, bottom)
        Suggestion.AddBelt, Suggestion.MakeItHarder ->
            SetPrefill(workingLoad(exercise.loadType, lastSets), planned.reps.last)
    }
}
