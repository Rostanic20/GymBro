package hr.rostanic20.gymbro.ui.workout

import hr.rostanic20.gymbro.domain.model.LoadType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadSettingsTest {

    @Test
    fun `an empty start weight means still calibrating`() {
        assertEquals(LoadSettings(startLoadKg = null, incrementKg = 5.0), validateLoadSettings(LoadType.WEIGHT, "", "5"))
    }

    @Test
    fun `weights accept a comma decimal`() {
        assertEquals(LoadSettings(startLoadKg = 17.5, incrementKg = 1.25), validateLoadSettings(LoadType.WEIGHT, "17,5", "1,25"))
    }

    @Test
    fun `a zero or negative start weight is rejected`() {
        assertFalse(isStartLoadValid(LoadType.WEIGHT, "0"))
        assertFalse(isStartLoadValid(LoadType.WEIGHT, "-10"))
        assertNull(validateLoadSettings(LoadType.WEIGHT, "0", "2.5"))
    }

    @Test
    fun `zero assistance is valid because it means bodyweight`() {
        assertTrue(isStartLoadValid(LoadType.ASSISTANCE, "0"))
        assertEquals(LoadSettings(startLoadKg = 0.0, incrementKg = 2.5), validateLoadSettings(LoadType.ASSISTANCE, "0", "2.5"))
    }

    @Test
    fun `the step is required and must be positive`() {
        assertNull(validateLoadSettings(LoadType.WEIGHT, "40", ""))
        assertNull(validateLoadSettings(LoadType.WEIGHT, "40", "0"))
        assertFalse(isIncrementValid("0"))
        assertFalse(isIncrementValid("abc"))
    }

    @Test
    fun `absurd values are rejected`() {
        assertNull(validateLoadSettings(LoadType.WEIGHT, "5000", "2.5"))
        assertNull(validateLoadSettings(LoadType.WEIGHT, "40", "100"))
    }

    @Test
    fun `bodyweight exercises have nothing to set`() {
        assertNull(validateLoadSettings(LoadType.BODYWEIGHT, "", "2.5"))
    }
}
