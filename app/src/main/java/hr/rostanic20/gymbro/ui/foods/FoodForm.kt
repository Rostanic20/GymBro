package hr.rostanic20.gymbro.ui.foods

import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.common.parseKg
import java.util.Locale

data class FoodForm(
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val unitName: String = "",
    val unitGrams: String = "",
)

private const val MAX_KCAL_PER_100G = 900.0
private const val MAX_MACRO_GRAMS_PER_100G = 100.0
private const val MAX_UNIT_GRAMS = 1000.0

fun Food.toForm(locale: Locale): FoodForm = FoodForm(
    name = name,
    kcal = formatKg(per100g.kcal, locale),
    protein = formatKg(per100g.proteinG, locale),
    carbs = formatKg(per100g.carbsG, locale),
    fat = formatKg(per100g.fatG, locale),
    unitName = unitName.orEmpty(),
    unitGrams = unitGrams?.let { formatKg(it, locale) }.orEmpty(),
)

fun FoodForm.validate(foodId: Long?): FoodDraft? {
    val trimmedName = name.trim()
    if (trimmedName.isEmpty()) return null
    val kcalValue = parseKg(kcal)?.takeIf { it in 0.0..MAX_KCAL_PER_100G } ?: return null
    val proteinValue = parseMacro(protein) ?: return null
    val carbsValue = parseMacro(carbs) ?: return null
    val fatValue = parseMacro(fat) ?: return null
    if (proteinValue + carbsValue + fatValue > MAX_MACRO_GRAMS_PER_100G) return null
    val unit = unitName.trim()
    val gramsPerUnit = if (unitGrams.isBlank()) {
        null
    } else {
        parseKg(unitGrams)?.takeIf { it > 0 && it <= MAX_UNIT_GRAMS } ?: return null
    }
    if (unit.isEmpty() != (gramsPerUnit == null)) return null
    return FoodDraft(
        id = foodId,
        name = trimmedName,
        per100g = Nutrition(kcalValue, proteinValue, carbsValue, fatValue),
        unitName = unit.ifEmpty { null },
        unitGrams = gramsPerUnit,
    )
}

private fun parseMacro(text: String): Double? =
    parseKg(text)?.takeIf { it in 0.0..MAX_MACRO_GRAMS_PER_100G }
