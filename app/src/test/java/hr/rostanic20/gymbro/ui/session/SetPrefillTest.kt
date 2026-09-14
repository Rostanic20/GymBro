package hr.rostanic20.gymbro.ui.session

import hr.rostanic20.gymbro.domain.Suggestion
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.util.plannedExercise
import org.junit.Assert.assertEquals
import org.junit.Test

class SetPrefillTest {

    private val bench = plannedExercise(1, "Barbell bench press", startLoadKg = 40.0)

    private fun slot(suggestion: Suggestion, sets: List<LoggedSet> = emptyList()) =
        SlotState(planned = bench, exercise = bench.exercise, sets = sets, lastSets = emptyList(), suggestion = suggestion)

    @Test
    fun `the last logged set prefills the next one`() {
        val logged = listOf(LoggedSet(1, 1, 1, 42.5, 6, 2))

        assertEquals(SetPrefill(42.5, 6), slot(Suggestion.Increase(42.5), logged).prefill())
    }

    @Test
    fun `a new weight starts at the bottom of the rep range`() {
        assertEquals(SetPrefill(42.5, 5), slot(Suggestion.Increase(42.5)).prefill())
    }

    @Test
    fun `repeating a weight prefills last time's first set`() {
        assertEquals(SetPrefill(40.0, 8), slot(Suggestion.Repeat(40.0, listOf(8, 8, 7))).prefill())
    }

    @Test
    fun `no known weight leaves the weight empty`() {
        assertEquals(SetPrefill(null, 5), slot(Suggestion.FirstTime(null)).prefill())
    }
}
