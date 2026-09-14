package hr.rostanic20.gymbro.ui.settings

import hr.rostanic20.gymbro.domain.KCAL_PER_G_FAT
import hr.rostanic20.gymbro.domain.KCAL_PER_G_PROTEIN
import hr.rostanic20.gymbro.domain.model.Profile

data class TargetsForm(
    val maintenanceKcal: String,
    val surplusKcal: String,
    val proteinG: String,
    val fatG: String,
)

data class Targets(
    val maintenanceKcal: Int,
    val surplusKcal: Int,
    val proteinG: Int,
    val fatG: Int,
)

private val MAINTENANCE_KCAL_RANGE = 1200..6000
private val SURPLUS_KCAL_RANGE = 0..1000
private val PROTEIN_G_RANGE = 40..400
private val FAT_G_RANGE = 20..250

fun Profile.toTargetsForm(): TargetsForm = TargetsForm(
    maintenanceKcal = maintenanceKcal.toString(),
    surplusKcal = surplusKcal.toString(),
    proteinG = proteinG.toString(),
    fatG = fatG.toString(),
)

fun TargetsForm.validate(): Targets? {
    val maintenance = maintenanceKcal.toIntIn(MAINTENANCE_KCAL_RANGE) ?: return null
    val surplus = surplusKcal.toIntIn(SURPLUS_KCAL_RANGE) ?: return null
    val protein = proteinG.toIntIn(PROTEIN_G_RANGE) ?: return null
    val fat = fatG.toIntIn(FAT_G_RANGE) ?: return null
    if (protein * KCAL_PER_G_PROTEIN + fat * KCAL_PER_G_FAT > maintenance) return null
    return Targets(maintenanceKcal = maintenance, surplusKcal = surplus, proteinG = protein, fatG = fat)
}

private fun String.toIntIn(range: IntRange): Int? = trim().toIntOrNull()?.takeIf { it in range }
