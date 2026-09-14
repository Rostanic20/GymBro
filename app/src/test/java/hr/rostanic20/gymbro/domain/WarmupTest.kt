package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.Progression
import org.junit.Assert.assertEquals
import org.junit.Test

class WarmupTest {

    private val bench = Exercise(1, "Barbell bench press", LoadType.WEIGHT, Progression.DOUBLE, 40.0, 2.5, 20.0)

    @Test
    fun `ramp is empty bar then 50, 70 and 85 percent rounded to plates`() {
        assertEquals(
            listOf(WarmupSet(20.0, 10), WarmupSet(30.0, 5), WarmupSet(42.5, 3), WarmupSet(50.0, 1)),
            bench.warmupRamp(workingLoadKg = 60.0),
        )
    }

    @Test
    fun `steps that collapse onto the bar are dropped`() {
        assertEquals(
            listOf(WarmupSet(20.0, 10), WarmupSet(27.5, 3), WarmupSet(35.0, 1)),
            bench.warmupRamp(workingLoadKg = 40.0),
        )
    }

    @Test
    fun `no step reaches the working weight`() {
        assertEquals(
            listOf(WarmupSet(20.0, 10)),
            bench.warmupRamp(workingLoadKg = 22.5),
        )
    }

    @Test
    fun `working at the empty bar needs no ramp`() {
        assertEquals(emptyList<WarmupSet>(), bench.warmupRamp(workingLoadKg = 20.0))
    }

    @Test
    fun `exercises without a bar get no ramp`() {
        val pulldown = bench.copy(name = "Lat pulldown, neutral grip", barWeightKg = null)

        assertEquals(emptyList<WarmupSet>(), pulldown.warmupRamp(workingLoadKg = 40.0))
    }
}
