package hr.rostanic20.gymbro.domain.model

import java.time.LocalDate

data class WorkoutSession(
    val id: Long,
    val dayId: Long,
    val date: LocalDate,
    val startedAtMillis: Long,
    val finishedAtMillis: Long?,
    val isDeload: Boolean,
    val note: String?,
    val restEndsAtMillis: Long?,
) {
    val isActive: Boolean get() = finishedAtMillis == null
}

data class SessionSummary(
    val id: Long,
    val dayId: Long,
    val date: LocalDate,
    val isDeload: Boolean,
    val note: String?,
    val setCount: Int,
)

data class LoggedSet(
    val id: Long,
    val exerciseId: Long,
    val slotPosition: Int,
    val loadKg: Double?,
    val reps: Int,
    val rir: Int?,
)

data class SetValues(
    val loadKg: Double?,
    val reps: Int,
    val rir: Int?,
)

data class TopSet(
    val loadKg: Double?,
    val reps: Int,
)

data class TopSetPoint(
    val date: LocalDate,
    val loadKg: Double?,
    val reps: Int,
)
