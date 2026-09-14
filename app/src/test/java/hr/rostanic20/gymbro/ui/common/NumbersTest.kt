package hr.rostanic20.gymbro.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class NumbersTest {

    private val croatian = Locale.forLanguageTag("hr-HR")

    @Test
    fun `kg keeps up to two decimals and no trailing zeros`() {
        assertEquals("40", formatKg(40.0, Locale.US))
        assertEquals("17.5", formatKg(17.5, Locale.US))
        assertEquals("1.25", formatKg(1.25, Locale.US))
    }

    @Test
    fun `kg uses the locale's decimal separator`() {
        assertEquals("2,5", formatKg(2.5, croatian))
    }

    @Test
    fun `counts use the locale's grouping`() {
        assertEquals("2,450", formatCount(2450, Locale.US))
        assertEquals("2.450", formatCount(2450, croatian))
    }

    @Test
    fun `parsing accepts either decimal separator`() {
        assertEquals(2.5, parseKg("2,5"))
        assertEquals(2.5, parseKg(" 2.5 "))
        assertEquals(40.0, parseKg("40"))
    }

    @Test
    fun `parsing rejects text that is not a finite number`() {
        assertNull(parseKg(""))
        assertNull(parseKg("abc"))
        assertNull(parseKg("NaN"))
        assertNull(parseKg("Infinity"))
    }
}
