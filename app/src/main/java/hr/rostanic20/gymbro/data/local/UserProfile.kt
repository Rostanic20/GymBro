package hr.rostanic20.gymbro.data.local

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val programStartEpochDay: Long? = null,
    val maintenanceKcal: Int = 2450,
    val surplusKcal: Int = 350,
    val kcalAdjustment: Int = 0,
    val proteinG: Int = 145,
    val fatG: Int = 75,
)
