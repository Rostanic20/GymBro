package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.TopSet
import hr.rostanic20.gymbro.util.plannedExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val bench = plannedExercise(1, "Barbell bench press", sets = 3, startLoadKg = 40.0)
    private val pullUp = plannedExercise(13, "Pull-up", sets = 3, loadType = LoadType.ASSISTANCE)
    private val legRaise = plannedExercise(25, "Hanging leg raise", sets = 2, loadType = LoadType.BODYWEIGHT)

    private fun sets(loadKg: Double?, vararg reps: Int, rir: Int? = 2): List<LoggedSet> =
        reps.mapIndexed { index, r -> LoggedSet(index.toLong(), 1, 1, loadKg, r, rir) }

    private fun suggest(
        planned: hr.rostanic20.gymbro.domain.model.PlannedExercise = bench,
        lastSets: List<LoggedSet>,
        week: Int? = 3,
        isDeload: Boolean = false,
    ) = suggestNext(planned, planned.exercise, lastSets, week, isDeload)

    @Test
    fun `first session after calibration starts at the saved start weight`() {
        assertEquals(Suggestion.FirstTime(40.0), suggest(lastSets = emptyList()))
    }

    @Test
    fun `calibration week never progresses`() {
        assertEquals(Suggestion.Calibrate(40.0), suggest(lastSets = emptyList(), week = 1))
        assertEquals(Suggestion.Calibrate(45.0), suggest(lastSets = sets(45.0, 8, 8, 8), week = 1))
        assertEquals(Suggestion.Calibrate(40.0), suggest(lastSets = emptyList(), week = null))
    }

    @Test
    fun `every set at the top of the range at target RIR adds the step`() {
        assertEquals(Suggestion.Increase(42.5), suggest(lastSets = sets(40.0, 8, 8, 8)))
    }

    @Test
    fun `one set short of the top repeats the weight`() {
        assertEquals(Suggestion.Repeat(40.0, listOf(8, 8, 7)), suggest(lastSets = sets(40.0, 8, 8, 7)))
    }

    @Test
    fun `grinding below the target RIR does not count as hitting the top`() {
        assertEquals(Suggestion.Repeat(40.0, listOf(8, 8, 8)), suggest(lastSets = sets(40.0, 8, 8, 8, rir = 1)))
    }

    @Test
    fun `an unlogged RIR does not block progress`() {
        assertEquals(Suggestion.Increase(42.5), suggest(lastSets = sets(40.0, 8, 8, 8, rir = null)))
    }

    @Test
    fun `fewer sets than planned repeats the weight`() {
        assertEquals(Suggestion.Repeat(40.0, listOf(8, 8)), suggest(lastSets = sets(40.0, 8, 8)))
    }

    @Test
    fun `the heaviest weight used is the one progression follows`() {
        val mixed = sets(40.0, 8, 8) + LoggedSet(9, 1, 1, 42.5, 6, 2)

        assertEquals(Suggestion.Repeat(42.5, listOf(6)), suggest(lastSets = mixed))
    }

    @Test
    fun `assistance comes off by the step and never goes below zero`() {
        assertEquals(Suggestion.Increase(17.5), suggest(pullUp, lastSets = sets(20.0, 8, 8, 8)))
        assertEquals(Suggestion.Increase(0.0), suggest(pullUp, lastSets = sets(1.0, 8, 8, 8)))
    }

    @Test
    fun `the top of the range with no assistance means adding a belt`() {
        assertEquals(Suggestion.AddBelt, suggest(pullUp, lastSets = sets(0.0, 8, 8, 8)))
    }

    @Test
    fun `assistance progression follows the lightest assistance used`() {
        val mixed = sets(20.0, 8) + LoggedSet(9, 1, 1, 17.5, 6, 2)

        assertEquals(Suggestion.Repeat(17.5, listOf(6)), suggest(pullUp, lastSets = mixed))
    }

    @Test
    fun `bodyweight exercises at the top need a harder variation`() {
        assertEquals(Suggestion.MakeItHarder, suggest(legRaise, lastSets = sets(null, 8, 8)))
    }

    @Test
    fun `deload keeps the last working weight`() {
        assertEquals(Suggestion.Deload(40.0), suggest(lastSets = sets(40.0, 8, 8, 8), isDeload = true))
    }

    @Test
    fun `two sessions running of lost reps at the same weight advises a deload`() {
        val history = listOf(TopSet(60.0, 5), TopSet(60.0, 6), TopSet(60.0, 7))

        assertTrue(needsDeload(LoadType.WEIGHT, history))
    }

    @Test
    fun `fewer reps after adding weight is progress, not a stall`() {
        val history = listOf(TopSet(62.5, 5), TopSet(60.0, 8), TopSet(60.0, 7))

        assertFalse(needsDeload(LoadType.WEIGHT, history))
    }

    @Test
    fun `one bad session is not enough`() {
        assertFalse(needsDeload(LoadType.WEIGHT, listOf(TopSet(60.0, 5), TopSet(60.0, 6), TopSet(60.0, 6))))
        assertFalse(needsDeload(LoadType.WEIGHT, listOf(TopSet(60.0, 5), TopSet(60.0, 6))))
    }

    @Test
    fun `for assistance, fewer reps with less help is not a stall`() {
        val history = listOf(TopSet(15.0, 5), TopSet(17.5, 6), TopSet(17.5, 7))

        assertFalse(needsDeload(LoadType.ASSISTANCE, history))
        assertTrue(needsDeload(LoadType.ASSISTANCE, listOf(TopSet(17.5, 5), TopSet(17.5, 6), TopSet(17.5, 7))))
    }
}
