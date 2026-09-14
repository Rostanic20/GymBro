package hr.rostanic20.gymbro.domain.model

import java.time.LocalDate

data class Profile(
    val programStart: LocalDate?,
    val maintenanceKcal: Int,
    val surplusKcal: Int,
    val kcalAdjustment: Int,
    val proteinG: Int,
    val fatG: Int,
)

data class NutritionTargets(
    val phase: NutritionPhase,
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
)

enum class NutritionPhase { MAINTENANCE, SURPLUS }
