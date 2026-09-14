package hr.rostanic20.gymbro.ui.foods

import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.Nutrition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FoodFormTest {

    private val whey = FoodForm(
        name = " Whey protein ",
        kcal = "400",
        protein = "78",
        carbs = "8",
        fat = "6",
        unitName = "scoop",
        unitGrams = "25",
    )

    @Test
    fun `a complete form becomes a draft`() {
        assertEquals(
            FoodDraft(4, "Whey protein", Nutrition(400.0, 78.0, 8.0, 6.0), "scoop", 25.0),
            whey.validate(foodId = 4),
        )
    }

    @Test
    fun `decimals accept a comma`() {
        assertEquals(13.2, whey.copy(protein = "13,2").validate(null)?.per100g?.proteinG)
    }

    @Test
    fun `the unit is optional but needs its grams`() {
        assertEquals(null, whey.copy(unitName = "", unitGrams = "").validate(null)?.unitName)
        assertNull(whey.copy(unitGrams = "").validate(null))
        assertNull(whey.copy(unitName = "").validate(null))
    }

    @Test
    fun `macros per 100 g cannot add up to more than 100 g`() {
        assertNull(whey.copy(protein = "80", carbs = "20", fat = "6").validate(null))
        assertEquals(100.0, FoodForm("Olive oil", "884", "0", "0", "100").validate(null)?.per100g?.fatG)
    }

    @Test
    fun `a name and every macro are required`() {
        assertNull(whey.copy(name = "  ").validate(null))
        assertNull(whey.copy(kcal = "").validate(null))
        assertNull(whey.copy(fat = "lots").validate(null))
        assertNull(whey.copy(kcal = "1200").validate(null))
    }

    @Test
    fun `an existing food fills the form in the user's locale`() {
        val oats = Food(2, "Oats", Nutrition(379.0, 13.2, 67.7, 6.5), null, null, isFavourite = false)

        val form = oats.toForm(Locale.forLanguageTag("hr-HR"))

        assertEquals("13,2", form.protein)
        assertEquals("", form.unitName)
        assertEquals(oats.per100g, form.validate(oats.id)?.per100g)
    }
}
