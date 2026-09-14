package hr.rostanic20.gymbro.domain.model

import java.time.LocalDate

data class BodyWeight(
    val date: LocalDate,
    val weightKg: Double,
)

data class WaistMeasurement(
    val date: LocalDate,
    val waistCm: Double,
)

enum class PhotoPose { FRONT, SIDE, BACK }

data class ProgressPhoto(
    val id: Long,
    val date: LocalDate,
    val pose: PhotoPose,
    val fileName: String,
)
