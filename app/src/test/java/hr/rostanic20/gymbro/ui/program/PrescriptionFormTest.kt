package hr.rostanic20.gymbro.ui.program

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrescriptionFormTest {

    @Test
    fun `a filled form becomes a prescription`() {
        assertEquals(Prescription(3, 8..12), PrescriptionForm("3", "8", "12").validate())
    }

    @Test
    fun `a single rep target is allowed`() {
        assertEquals(Prescription(5, 5..5), PrescriptionForm("5", "5", "5").validate())
    }

    @Test
    fun `reps cannot count down`() {
        assertNull(PrescriptionForm("3", "12", "8").validate())
    }

    @Test
    fun `zero sets and empty fields are refused`() {
        assertNull(PrescriptionForm("0", "8", "12").validate())
        assertNull(PrescriptionForm("", "8", "12").validate())
        assertNull(PrescriptionForm("3", "8", "").validate())
    }

    @Test
    fun `absurd numbers are refused`() {
        assertNull(PrescriptionForm("99", "8", "12").validate())
        assertNull(PrescriptionForm("3", "8", "99").validate())
    }
}
