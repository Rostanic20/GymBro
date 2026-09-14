package hr.rostanic20.gymbro.ui.session

import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.SetValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetInputTest {

    @Test
    fun `weight, reps and RIR make a set`() {
        assertEquals(SetValues(42.5, 8, 2), validateSetInput(LoadType.WEIGHT, "42,5", "8", "2"))
    }

    @Test
    fun `RIR is optional`() {
        assertEquals(SetValues(40.0, 6, null), validateSetInput(LoadType.WEIGHT, "40", "6", ""))
    }

    @Test
    fun `a weighted set needs a positive weight`() {
        assertNull(validateSetInput(LoadType.WEIGHT, "", "8", "2"))
        assertNull(validateSetInput(LoadType.WEIGHT, "0", "8", "2"))
    }

    @Test
    fun `zero assistance is a bodyweight rep`() {
        assertEquals(SetValues(0.0, 6, 1), validateSetInput(LoadType.ASSISTANCE, "0", "6", "1"))
    }

    @Test
    fun `bodyweight sets ignore the weight field`() {
        assertEquals(SetValues(null, 12, null), validateSetInput(LoadType.BODYWEIGHT, "whatever", "12", ""))
    }

    @Test
    fun `reps and RIR must be sane whole numbers`() {
        assertNull(validateSetInput(LoadType.WEIGHT, "40", "0", "2"))
        assertNull(validateSetInput(LoadType.WEIGHT, "40", "8.5", "2"))
        assertNull(validateSetInput(LoadType.WEIGHT, "40", "8", "-1"))
        assertNull(validateSetInput(LoadType.WEIGHT, "40", "8", "11"))
    }
}
