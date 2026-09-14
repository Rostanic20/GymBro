package hr.rostanic20.gymbro.ui.settings

import hr.rostanic20.gymbro.util.defaultProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetsFormTest {

    private val valid = TargetsForm(maintenanceKcal = "2450", surplusKcal = "350", proteinG = "145", fatG = "75")

    @Test
    fun `the program's own targets are valid`() {
        assertEquals(Targets(2450, 350, 145, 75), valid.validate())
    }

    @Test
    fun `the saved profile round-trips into the form`() {
        assertEquals(valid, defaultProfile.toTargetsForm())
    }

    @Test
    fun `blank or non-numeric fields are invalid`() {
        assertNull(valid.copy(maintenanceKcal = "").validate())
        assertNull(valid.copy(proteinG = "lots").validate())
    }

    @Test
    fun `values outside a sane range are invalid`() {
        assertNull(valid.copy(maintenanceKcal = "900").validate())
        assertNull(valid.copy(surplusKcal = "2000").validate())
        assertNull(valid.copy(fatG = "5").validate())
    }

    @Test
    fun `protein and fat cannot eat more than the whole calorie budget`() {
        assertNull(TargetsForm(maintenanceKcal = "1500", surplusKcal = "0", proteinG = "250", fatG = "120").validate())
    }
}
