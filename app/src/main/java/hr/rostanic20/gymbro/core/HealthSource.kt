package hr.rostanic20.gymbro.core

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class HealthSummary(val steps: Long?, val sleep: Duration?)

interface HealthSource {
    val readPermissions: Set<String>
    fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun summaryFor(date: LocalDate, now: ZonedDateTime): HealthSummary
}
